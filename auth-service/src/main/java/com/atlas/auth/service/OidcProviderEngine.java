package com.atlas.auth.service;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/23 13:37
 */

import com.atlas.auth.domain.dto.OidcProviderSettings;
import com.atlas.auth.domain.dto.OidcUserInfoResult;
import com.atlas.auth.domain.dto.SsoProviderAuthorizeUrlResponse;
import com.atlas.auth.domain.entity.SsoProvider;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.security.properties.SecurityProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Description 标准 OIDC 认证引擎核心处理器
 * @Author ys
 * @Date 2026/6/23 13:37
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OidcProviderEngine {

    private final RestClient proxyRestClient;

    private final RestClient defaultRestClient;

    private final SsoProviderService ssoProviderService;

    private final SecurityProperties securityProperties;


    // 自发现元数据
    public OidcMetadata fetchMetadata(String provider, String issuerUrl) {
        String discoveryUrl = issuerUrl.endsWith("/") ? issuerUrl + ".well-known/openid-configuration" : issuerUrl + "/.well-known/openid-configuration";
        return getClient(provider).get()
                .uri(discoveryUrl)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    String errorBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                    log.error("[{}] 抓取 OIDC 发现端点失败 [URL: {}, Status: {}, Body: {}]",
                            provider, request.getURI(), response.getStatusCode(), errorBody);
                    throw new BusinessException("加载 OIDC 元数据失败");
                })
                .body(OidcMetadata.class);
    }

    public SsoProviderAuthorizeUrlResponse buildAuthorizeUrl(String provider, OidcProviderSettings settings, Map<String, String> params) {
        OidcMetadata metadata = fetchMetadata(provider, settings.issuerUrl());
        String redirectUrl = securityProperties.getUiUrl() + settings.redirectUrl();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(metadata.authorizationEndpoint)
                .queryParam("client_id", settings.clientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUrl)
                .queryParam("scope", settings.scope());

        // 注入静态额外参数
        if (settings.extraParams() != null && settings.extraParams().authorize() != null) {
            settings.extraParams().authorize().forEach(builder::queryParam);
        }
        // 注入动态额外参数
        if (params != null && !params.isEmpty()) {
            params.forEach(builder::queryParam);
        }
        String uriString = builder.build().encode().toUriString();
        return new SsoProviderAuthorizeUrlResponse(uriString, true);
    }


    public OidcUserInfoResult fetchUserInfo(String provider, OidcProviderSettings settings, String code, String codeVerifier) {
        OidcMetadata metadata = fetchMetadata(provider, settings.issuerUrl());
        String redirectUrl = securityProperties.getUiUrl() + settings.redirectUrl();
        // 请求参数
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", settings.clientId());
        body.add("client_secret", settings.clientSecret());
        body.add("code", code);
        body.add("redirect_uri", redirectUrl);
        body.add("grant_type", "authorization_code");

        if (!StringUtils.hasText(codeVerifier)) {
            throw new BusinessException("OIDC 安全校验失败：缺失 code_verifier");
        }
        body.add("code_verifier", codeVerifier);
        // 获取token
        OidcTokenResponse tokenResponse = getClient(provider).post()
                .uri(metadata.tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    String errorBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                    log.error("[{}] OIDC 令牌验证失败 [URL: {}, Status: {}, Body: {}]",
                            provider, request.getURI(), response.getStatusCode(), errorBody);
                    throw new BusinessException("OIDC 授权验证失败");
                })
                .body(OidcTokenResponse.class);

        if (!StringUtils.hasText(tokenResponse.idToken)) {
            throw new BusinessException("OIDC 认证失败：未返回合法的 id_token");
        }

        try {
            NimbusJwtDecoder jwtDecoder = this.buildJwtDecoder(provider, metadata);
            Jwt jwt = jwtDecoder.decode(tokenResponse.idToken);
            if (!jwt.getAudience().contains(settings.clientId())) {
                throw new BusinessException("安全审计失败：票据受众不匹配");
            }
            Map<String, Object> claims = jwt.getClaims();
            OidcUserInfoResult userInfo = JsonUtils.convert(claims, OidcUserInfoResult.class);
            userInfo.setProvider(provider);
            userInfo.setExtraInfo(claims);
            return userInfo;
        } catch (Exception e) {
            log.error("[{}] OIDC 凭证强验失败", provider, e);
            throw new BusinessException("OIDC 认证票据核验失败");
        }
    }

    private RestClient getClient(String provider) {
        SsoProvider ssoProvider = ssoProviderService.getProvider(provider);
        if (ssoProvider == null) {
            log.warn("未找到提供商配置: {}, 使用默认客户端", provider);
        }
        return (ssoProvider != null && ssoProvider.getUseProxy()) ? proxyRestClient : defaultRestClient;
    }

    private NimbusJwtDecoder buildJwtDecoder(String provider, OidcMetadata metadata) {
        try {
            // 获取密钥json
            String jwksJson = getClient(provider)
                    .get()
                    .uri(metadata.jwksUri)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .exchange((request, response) -> {
                        try (InputStream is = response.getBody()) {
                            byte[] bytes = StreamUtils.copyToByteArray(is);
                            return new String(bytes, StandardCharsets.UTF_8);
                        }
                    });

            // 解析公钥集并注入静态内存源
            JWKSet jwkSet = JWKSet.parse(jwksJson);
            JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

            // 动态提取元数据支持的全部算法列表
            List<String> algsSupported = metadata.idTokenSigningAlgValuesSupported;
            Set<JWSAlgorithm> expectedJWSAlgs = new HashSet<>();

            if (algsSupported != null && !algsSupported.isEmpty()) {
                for (String alg : algsSupported) {
                    if (!JWSAlgorithm.NONE.getName().equalsIgnoreCase(alg)) {
                        expectedJWSAlgs.add(JWSAlgorithm.parse(alg));
                    }
                }
            }

            if (expectedJWSAlgs.isEmpty()) {
                expectedJWSAlgs.add(JWSAlgorithm.RS256);
            }

            // 组装选择器与处理器
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlgs, jwkSource);
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSKeySelector(keySelector);

            // 通过构造函数返回定制化的解码器
            return new NimbusJwtDecoder(jwtProcessor);

        } catch (Exception e) {
            log.error("构建 OIDC 本地 JWT 解码器失败", e);
            throw new BusinessException("初始化 OIDC 安全校验组件失败");
        }
    }

    private record OidcTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("id_token") String idToken,
            @JsonProperty("expires_in") Long expiresIn
    ) {
    }

    public record OidcMetadata(
            @JsonProperty("authorization_endpoint")
            String authorizationEndpoint,

            @JsonProperty("token_endpoint")
            String tokenEndpoint,

            @JsonProperty("jwks_uri")
            String jwksUri,

            @JsonProperty("userinfo_endpoint")
            String userinfoEndpoint,

            @JsonProperty("end_session_endpoint")
            String endSessionEndpoint,

            @JsonProperty("id_token_signing_alg_values_supported")
            List<String> idTokenSigningAlgValuesSupported
    ) {
    }

}
