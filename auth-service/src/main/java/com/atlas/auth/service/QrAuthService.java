package com.atlas.auth.service;

import com.atlas.auth.config.security.oauth2.CustomClientSettings;
import com.atlas.auth.domain.dto.OAuth2ProviderAuthorizeUrlResponse;
import com.atlas.auth.domain.dto.OAuth2ProviderSettings;
import com.atlas.auth.domain.vo.QrAuthStatusVO;
import com.atlas.auth.domain.vo.QrAuthTicketVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.common.core.constant.CommonConstant;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.security.properties.SecurityProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrAuthService {

    private final RedisHelper redisHelper;

    private final SecurityProperties securityProperties;

    private final RegisteredClientRepository registeredClientRepository;

    private final SsoProviderService ssoProviderService;

    private final OAuth2ProviderEngine oAuth2ProviderEngine;

    private final RestClient defaultRestClient;

    /**
     * 二维码有效期（秒）
     */
    private static final long EXPIRE_SECONDS = 300;

    private static final String PENDING = "PENDING";
    private static final String SCANNED = "SCANNED";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String EXPIRED = "EXPIRED";

    private static final String QR_SCENE_KEY = "auth:qr:scene:";

    private static final String SELF_PROVIDER = "atlas";

    public QrAuthTicketVO ticket(String clientId, String redirectUri, String scope, String state, String codeChallenge,String codeChallengeMethod) {
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if(registeredClient == null){
            throw new BusinessException("客户端不存在");
        }
        // 检查传入的 redirectUri 是否在注册的允许列表中
        if (!registeredClient.getRedirectUris().contains(redirectUri)) {
            throw new BusinessException("非法重定向地址");
        }
        // 检查传入的 scope 是否属于该客户端允许的范围
        Set<String> authorizedScopes = registeredClient.getScopes();
        for (String s : scope.split(" ")) {
            if (!authorizedScopes.contains(s)) {
                throw new BusinessException("包含未授权的 Scope: " + s);
            }
        }

        String sceneId = UUID.randomUUID().toString().replaceAll("-", "");

        String redisKey = QR_SCENE_KEY + sceneId;
        Map<String, Object> context = new HashMap<>();
        context.put("status", PENDING);
        context.put("clientId", clientId);
        context.put("clientName", registeredClient.getClientName());
        context.put("redirectUri", redirectUri);
        context.put("scope", scope);
        context.put("state", state);
        if (StringUtils.hasText(codeChallenge)) {
            context.put("code_challenge", codeChallenge);
            // 如果客户端没传 method，按照 OAuth2 规范默认使用 S256
            context.put("code_challenge_method", StringUtils.hasText(codeChallengeMethod) ? codeChallengeMethod : "S256");
        }
        redisHelper.addHash(redisKey,context,Duration.ofSeconds(EXPIRE_SECONDS));

        ClientSettings clientSettings = registeredClient.getClientSettings();
        String logoUri = clientSettings.getSetting(CustomClientSettings.LOGO_URI);
        log.info("生成二维码登录凭证: {}", sceneId);
        String url = UriComponentsBuilder.fromPath("/oauth2/qr/scan")
                .queryParam("scene_id", sceneId)
                .queryParam("client_name", registeredClient.getClientName())
                .queryParam("logo_uri", logoUri)
                .queryParam("scope", scope)
                .queryParam("redirect_uri", redirectUri)
                .build()
                .toUriString();
        String qrUrl = securityProperties.getUiUrl() + url;
        return QrAuthTicketVO
                .builder()
                .sceneId(sceneId)
                .qrUrl(qrUrl)
                .expireSeconds(EXPIRE_SECONDS)
                .build();
    }

    public void scan(String sceneId){
        String redisKey = QR_SCENE_KEY + sceneId;
        Map<String, Object> context = redisHelper.getHashAll(redisKey);
        if (context == null || context.isEmpty()) {
            throw new BusinessException("二维码已过期，请刷新");
        }
        String currentStatus = (String) context.get("status");
        if (PENDING.equals(currentStatus)) {
            redisHelper.addHash(redisKey, "status", SCANNED);
        }
    }

    public void confirm(String sceneId,String tokenId){
        String redisKey = QR_SCENE_KEY + sceneId;
        Map<String, Object> context = redisHelper.getHashAll(redisKey);
        if (context == null || context.isEmpty()) {
            throw new BusinessException("二维码不存在或已过期");
        }
        String currentStatus = (String)context.get("status");

        if(EXPIRED.equals(currentStatus)){
            throw new BusinessException("二维码已过期");
        }

        if (CONFIRMED.equals(currentStatus)) {
            throw new BusinessException("请勿重复确认");
        }

        OAuth2ProviderSettings settings = ssoProviderService.getSettings(SELF_PROVIDER, SsoProviderProtocol.OAUTH2);
        Map<String, String> extraParams = new HashMap<>(Map.of(
                "format", "json",
                "login_mode", "qr"
        ));
        if (context.containsKey("code_challenge")) {
            extraParams.put("code_challenge", context.get("code_challenge").toString());
            extraParams.put("code_challenge_method", context.get("code_challenge_method").toString());
        }

        OAuth2ProviderAuthorizeUrlResponse response = oAuth2ProviderEngine.buildAuthorizeUrl(settings, extraParams);
        AuthorizationUrlResponse authorizationResponse = defaultRestClient
                .get()
                .uri(response.url())
                .header(CommonConstant.TOKEN_ID,tokenId)
                .retrieve()
                .body(AuthorizationUrlResponse.class);

        String code = authorizationResponse.code();
        // 将生成的 code 存回 Redis，供第三方系统的前端轮询获取 一分钟内未获取则直接失效
        Map<String, Object> updates = new HashMap<>();
        updates.put("code", code);
        updates.put("status", CONFIRMED);
        redisHelper.addHash(redisKey, updates, Duration.ofMinutes(1));
        log.info("qr confirm, sceneId: {} code: {}",sceneId, code);
    }

    public QrAuthStatusVO status(String sceneId) {
        String redisKey = QR_SCENE_KEY + sceneId;
        Map<String, Object> map = redisHelper.getHashAll(redisKey);
        if (map == null || map.isEmpty()) {
            return QrAuthStatusVO
                    .builder()
                    .status(EXPIRED)
                    .build();
        }
        String status = (String) map.get("status");
        String authCode = (String) map.get("code");
        String clientName = (String) map.get("clientName");
        String state = (String) map.get("state");
        log.debug("检查二维码状态 sceneId: {}, 状态: {}", sceneId, status);
        return QrAuthStatusVO
                .builder()
                .status(status)
                .code(authCode)
                .state(state)
                .clientName(clientName)
                .build();
    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AuthorizationUrlResponse(
            String code,
            String state,
            String message
    ) {}

}
