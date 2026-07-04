package com.atlas.auth.service;


import com.atlas.auth.domain.dto.OAuth2ApplicationSaveDTO;
import com.atlas.auth.domain.entity.OAuth2Application;
import com.atlas.auth.domain.vo.OAuth2ApplicationCreateVO;
import com.atlas.auth.domain.vo.OAuth2ApplicationVO;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.security.utils.TicketGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2ApplicationFacade {

    private final OAuth2ApplicationService applicationService;

    private final RegisteredClientRepository registeredClientRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     * 保存应用（兼容创建和修改）
     */
    @Transactional(rollbackFor = Exception.class)
    public OAuth2ApplicationCreateVO save(OAuth2ApplicationSaveDTO saveDTO) {
        if (saveDTO.id() == null){
            return createApplication(saveDTO);
        } else {
            updateApplication(saveDTO);
            return null;
        }
    }

    public OAuth2ApplicationVO getApplicationDetail(Long applicationId){
        Objects.requireNonNull(applicationId, "applicationId must not be null");
        OAuth2Application app = applicationService.getById(applicationId);
        if (app == null) {
            throw new BusinessException("对应应用不存在");
        }
        RegisteredClient registeredClient = registeredClientRepository.findById(app.getRegisteredClientId());
        if (registeredClient == null) {
            throw new BusinessException("应用关联的框架底层客户端数据丢失，请检查数据一致性");
        }
        return OAuth2ApplicationVO.of(app, registeredClient);
    }

    private OAuth2ApplicationCreateVO createApplication(OAuth2ApplicationSaveDTO saveDTO){
        log.info("正在创建 OAuth2 应用，名称: {}", saveDTO.applicationName());
        // oauth2_registered_client 物理主键
        String registeredClientId = UUID.randomUUID().toString().replace("-", "");
        // 暴露给用户的 clientId
        String clientId = TicketGenerator.generate(20);
        // 暴露给用户的 clientSecret
        String rawSecret = TicketGenerator.generate(32);

        RegisteredClient.Builder clientBuilder = RegisteredClient.withId(registeredClientId)
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(rawSecret))
                .clientName(saveDTO.applicationName())
                // 核心标准模式：授权码模式 + 刷新令牌模式
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                // 配置 Token 策略
                .tokenSettings(
                        TokenSettings.builder()
                                // 访问令牌有效期 1 天
                                .accessTokenTimeToLive(Duration.ofDays(1))
                                // 刷新令牌有效期 7 天
                                .refreshTokenTimeToLive(Duration.ofDays(7))
                                // 授权码有效期 5 分钟
                                .authorizationCodeTimeToLive(Duration.ofMinutes(5))
                                // 设备码有效期 5 分钟
                                .deviceCodeTimeToLive(Duration.ofMinutes(5))
                                // 允许复用 Refresh Token (不启用轮转策略)
                                .reuseRefreshTokens(true)
                                // 指定 Access Token 的格式为自包含型 (即标准 JWT)
                                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                                // OIDC 场景下 ID Token 的签名算法指定为 RS256
                                .idTokenSignatureAlgorithm(SignatureAlgorithm.RS256)
                                .build()
                );


        // 根据动态追加设备码授权模式
        if (Boolean.TRUE.equals(saveDTO.allowDeviceFlow())) {
            clientBuilder.authorizationGrantType(AuthorizationGrantType.DEVICE_CODE);
            clientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        }
        // 回调地址
        if (saveDTO.redirectUri() != null) {
            saveDTO.redirectUri().forEach(clientBuilder::redirectUri);
        }
        // 授权范围
        if (saveDTO.scopes() != null) {
            saveDTO.scopes().forEach(clientBuilder::scope);
        }
        // 基础安全配置（强制开启现代高度安全的 PKCE 校验流）
        clientBuilder.clientSettings(ClientSettings.builder()
                .requireProofKey(true)
                .requireAuthorizationConsent(true)
                .build());

        registeredClientRepository.save(clientBuilder.build());

        // 保存 (oauth2_application)
        OAuth2Application oAuth2Application = new OAuth2Application();
        oAuth2Application.setId(IdGen.genId());
        oAuth2Application.setRegisteredClientId(registeredClientId);
        oAuth2Application.setClientId(clientId);
        oAuth2Application.setApplicationName(saveDTO.applicationName());
        oAuth2Application.setLogoUrl(saveDTO.logoUrl());
        oAuth2Application.setHomePageUrl(saveDTO.homePageUrl());
        oAuth2Application.setDescription(saveDTO.description());
        applicationService.save(oAuth2Application);
        log.info("OAuth2 应用创建成功, id: {}, clientId: {}", oAuth2Application.getId(), clientId);
        return new OAuth2ApplicationCreateVO(clientId, rawSecret);
    }


    private void updateApplication(OAuth2ApplicationSaveDTO saveDTO){
        Long applicationId = saveDTO.id();
        log.info("正在修改 OAuth2 应用，ID: {}", applicationId);
        OAuth2Application oauth2Application = applicationService.getById(applicationId);
        if (oauth2Application == null) {
            throw new BusinessException("当前编辑的应用不存在");
        }
        RegisteredClient registeredClient = registeredClientRepository.findById(oauth2Application.getRegisteredClientId());
        if (registeredClient == null) {
            throw new BusinessException("应用["+oauth2Application.getApplicationName()+"]关联的OAuth2客户端数据丢失，请检查数据一致性");
        }
        RegisteredClient updatedClient = RegisteredClient.from(registeredClient)
                .clientName(saveDTO.applicationName())
                // 清理旧的回调和范围，重新注入新配置
                .redirectUris(uris -> { uris.clear(); if (saveDTO.redirectUri() != null) uris.addAll(saveDTO.redirectUri()); })
                .scopes(scopes -> { scopes.clear(); if (saveDTO.scopes() != null) scopes.addAll(saveDTO.scopes()); })
                // 动态重组授权码、刷新模式与设备码模式
                .authorizationGrantTypes(types -> {
                    types.clear();
                    types.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                    types.add(AuthorizationGrantType.REFRESH_TOKEN);
                    if (Boolean.TRUE.equals(saveDTO.allowDeviceFlow())) {
                        types.add(AuthorizationGrantType.DEVICE_CODE);
                    }
                })
                .build();
        registeredClientRepository.save(updatedClient);

        // 同步更新 oauth2_application
        oauth2Application.setApplicationName(saveDTO.applicationName());
        oauth2Application.setLogoUrl(saveDTO.logoUrl());
        oauth2Application.setHomePageUrl(saveDTO.homePageUrl());
        oauth2Application.setDescription(saveDTO.description());

        applicationService.updateById(oauth2Application);
        log.info("OAuth2 应用配置更新成功, id: {}, applicationName: {}", oauth2Application.getId(), oauth2Application.getApplicationName());
    }
}
