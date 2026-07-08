package com.atlas.auth.service;


import com.atlas.auth.domain.dto.OAuth2ClientApplicationQueryDTO;
import com.atlas.auth.domain.dto.OAuth2ClientApplicationSaveDTO;
import com.atlas.auth.domain.entity.OAuth2ClientApplication;
import com.atlas.auth.domain.entity.OAuth2ClientSecret;
import com.atlas.auth.domain.vo.OAuth2ClientApplicationCreateVO;
import com.atlas.auth.domain.vo.OAuth2ClientApplicationVO;
import com.atlas.auth.mapper.OAuth2RegisteredClientMapper;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.common.mybatis.handler.DataPermissionContext;
import com.atlas.security.utils.TicketGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
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
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2ClientApplicationFacadeService {

    private final OAuth2ClientApplicationService oAuth2ClientApplicationService;

    private final RegisteredClientRepository registeredClientRepository;

    private final OAuth2RegisteredClientMapper oAuth2RegisteredClientMapper;

    private final OAuth2ClientSecretService oAuth2ClientSecretService;

    private final PasswordEncoder passwordEncoder;

    /**
     * 保存应用（兼容创建和修改）
     */
    @Transactional(rollbackFor = Exception.class)
    public OAuth2ClientApplicationCreateVO save(OAuth2ClientApplicationSaveDTO saveDTO) {
        if (saveDTO.id() == null){
            return createApplication(saveDTO);
        } else {
            updateApplication(saveDTO);
            return null;
        }
    }

    public PageInfo<OAuth2ClientApplicationVO> getPage(OAuth2ClientApplicationQueryDTO queryDTO){
        try (DataPermissionContext ctx = DataPermissionContext.open()){
            Integer pageNum = queryDTO.getPageNum();
            Integer pageSize = queryDTO.getPageSize();
            PageHelper.startPage(pageNum, pageSize);
            QueryWrapper<OAuth2ClientApplication> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByAsc("create_time");
            List<OAuth2ClientApplication> applications = oAuth2ClientApplicationService.list(queryWrapper);
            if(CollectionUtils.isEmpty(applications)){
                return PageInfo.emptyPageInfo();
            }
            PageInfo<OAuth2ClientApplication> entityPageInfo = new PageInfo<>(applications);
            return entityPageInfo.convert(OAuth2ClientApplicationVO::of);
        }
    }

    public OAuth2ClientApplicationVO getApplicationDetail(Long applicationId){
        Objects.requireNonNull(applicationId, "applicationId must not be null");
        OAuth2ClientApplication app = oAuth2ClientApplicationService.getById(applicationId);
        if (app == null) {
            throw new BusinessException("应用不存在");
        }
        RegisteredClient registeredClient = registeredClientRepository.findById(app.getRegisteredClientId());
        if (registeredClient == null) {
            throw new BusinessException("应用关联的oauth2客户端数据丢失，请检查数据一致性");
        }
        List<OAuth2ClientSecret> oAuth2ClientSecrets = oAuth2ClientSecretService.listValidSecretsByRegisteredClientId(registeredClient.getId());
        return OAuth2ClientApplicationVO.of(app, registeredClient, oAuth2ClientSecrets);
    }

    @Transactional
    public void deleteByApplicationId(Long id){
        log.info("正在删除 OAuth2 应用，ID: {}", id);
        OAuth2ClientApplication app = oAuth2ClientApplicationService.getById(id);
        if (app == null) {
            log.warn("删除 OAuth2 应用失败，应用不存在，ID: {}", id);
            throw new BusinessException("应用不存在或已被删除");
        }
        // 删除oauth2客户端密钥信息
        oAuth2ClientSecretService.removeByRegisteredClientId(app.getRegisteredClientId());
        // 删除oauth2客户端对应的所有token
        oAuth2RegisteredClientMapper.deleteAllClientToken(app.getRegisteredClientId());
        // 删除oauth2标准客户端信息
        oAuth2RegisteredClientMapper.deleteById(app.getRegisteredClientId());

        // 删除客户端应用主表信息
        boolean delFlag = oAuth2ClientApplicationService.removeById(id);
        if(!delFlag){
            log.error("删除 OAuth2 应用主表记录失败，ID: {}", id);
            throw new BusinessException("删除应用失败，请稍后重试");
        }

        log.info("OAuth2 应用删除成功，ID: {}", id);
    }

    @Transactional(rollbackFor = Exception.class)
    public OAuth2ClientApplicationCreateVO addClientSecret(Long applicationId) {
        Objects.requireNonNull(applicationId, "applicationId must not be null");
        log.info("正在为 OAuth2 应用生成新密钥，ID: {}", applicationId);
        OAuth2ClientApplication app = oAuth2ClientApplicationService.getById(applicationId);
        if (app == null) {
            throw new BusinessException("应用不存在");
        }
        String registeredClientId = app.getRegisteredClientId();
        long countValidSecrets = oAuth2ClientSecretService.countValidSecrets(registeredClientId);
        if(countValidSecrets >= 2 ){
            throw new BusinessException("每个应用最多只能同时存在 2 个有效密钥，请先删除旧密钥后再生成新密钥");
        }
        RegisteredClient registeredClient = registeredClientRepository.findById(registeredClientId);
        if (registeredClient == null) {
            throw new BusinessException("应用关联的oauth2客户端数据丢失，请检查数据一致性");
        }
        String rawSecret = TicketGenerator.generate(32);
        // 保存密钥
        saveClientSecret(registeredClientId, rawSecret);
        return new OAuth2ClientApplicationCreateVO(app.getId(),registeredClient.getClientId(), rawSecret);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteClientSecret(Long clientSecretId){
        Objects.requireNonNull(clientSecretId, "clientSecretId must not be null");
        log.info("正在删除 OAuth2 应用密钥，ID: {}", clientSecretId);
        OAuth2ClientSecret targetSecret = oAuth2ClientSecretService.getById(clientSecretId);
        if (targetSecret == null) {
            throw new BusinessException("该密钥不存在或已被删除");
        }
        String registeredClientId = targetSecret.getRegisteredClientId();
        List<OAuth2ClientSecret> validSecrets = oAuth2ClientSecretService.listValidSecretsByRegisteredClientId(registeredClientId);
        if(CollectionUtils.isEmpty(validSecrets) || validSecrets.size() == 1){
            throw new BusinessException("每个应用必须保留至少 1 个有效密钥，无法继续删除");
        }
        boolean isDeleted = oAuth2ClientSecretService.removeById(clientSecretId);
        if (!isDeleted) {
            throw new BusinessException("删除密钥失败，请稍后重试");
        }
        // 找出剩下的那一个密钥，并同步刷新到 Spring Security Oauth2 核心表中
        OAuth2ClientSecret remainingSecret = validSecrets.stream()
                .filter(s -> !s.getId().equals(clientSecretId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到剩余的有效密钥，数据可能不一致"));
        RegisteredClient registeredClient = registeredClientRepository.findById(registeredClientId);
        if (registeredClient != null) {
            RegisteredClient updatedClient = RegisteredClient.from(registeredClient)
                    .clientSecret(remainingSecret.getClientSecret()) // 将剩下这一个的加密串同步过去
                    .build();
            registeredClientRepository.save(updatedClient);
        }

        log.info("OAuth2 应用密钥删除成功，ID: {}，已同步剩余密钥给核心框架", clientSecretId);
    }

    private OAuth2ClientApplicationCreateVO createApplication(OAuth2ClientApplicationSaveDTO saveDTO){
        log.info("正在创建 OAuth2 应用，名称: {}", saveDTO.applicationName());
        // oauth2_registered_client 物理主键
        String registeredClientId = UUID.randomUUID().toString().replace("-", "");
        // 暴露给用户的 clientId
        String clientId = TicketGenerator.generate(20);
        // 暴露给用户的 clientSecret
        String rawSecret = TicketGenerator.generate(32);
        // 加密后的密钥
        String clientSecret = passwordEncoder.encode(rawSecret);
        RegisteredClient.Builder clientBuilder = RegisteredClient.withId(registeredClientId)
                .clientId(clientId)
                .clientSecret(clientSecret)
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

        // 保存密钥
        saveClientSecret(registeredClientId, rawSecret);

        // 保存 (oauth2_application)
        OAuth2ClientApplication oAuth2Application = new OAuth2ClientApplication();
        oAuth2Application.setId(IdGen.genId());
        oAuth2Application.setRegisteredClientId(registeredClientId);
        oAuth2Application.setClientId(clientId);
        oAuth2Application.setApplicationName(saveDTO.applicationName());
        oAuth2Application.setLogoUrl(saveDTO.logoUrl());
        oAuth2Application.setHomePageUrl(saveDTO.homePageUrl());
        oAuth2Application.setPrivacyPolicyUrl(saveDTO.privacyPolicyUrl());
        oAuth2Application.setTermsServiceUrl(saveDTO.termsServiceUrl());
        oAuth2Application.setDeveloperName(saveDTO.developerName());
        oAuth2Application.setDeveloperEmail(saveDTO.developerEmail());
        oAuth2Application.setDescription(saveDTO.description());
        oAuth2ClientApplicationService.save(oAuth2Application);
        log.info("OAuth2 应用创建成功, id: {}, clientId: {}", oAuth2Application.getId(), clientId);
        return new OAuth2ClientApplicationCreateVO(oAuth2Application.getId(),clientId, rawSecret);
    }


    private void updateApplication(OAuth2ClientApplicationSaveDTO saveDTO){
        Long applicationId = saveDTO.id();
        log.info("正在修改 OAuth2 应用，ID: {}", applicationId);
        OAuth2ClientApplication oauth2Application = oAuth2ClientApplicationService.getById(applicationId);
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
        oauth2Application.setPrivacyPolicyUrl(saveDTO.privacyPolicyUrl());
        oauth2Application.setTermsServiceUrl(saveDTO.termsServiceUrl());
        oauth2Application.setDeveloperName(saveDTO.developerName());
        oauth2Application.setDeveloperEmail(saveDTO.developerEmail());
        oauth2Application.setDescription(saveDTO.description());

        oAuth2ClientApplicationService.updateById(oauth2Application);
        log.info("OAuth2 应用配置更新成功, id: {}, applicationName: {}", oauth2Application.getId(), oauth2Application.getApplicationName());
    }

    private void saveClientSecret(String registeredClientId, String rawSecret){
        // 保存密钥
        String hint = rawSecret.length() > 4 ? rawSecret.substring(rawSecret.length() - 4) : rawSecret;
        OAuth2ClientSecret clientSecretEntity = new OAuth2ClientSecret();
        clientSecretEntity.setId(IdGen.genId());
        clientSecretEntity.setRegisteredClientId(registeredClientId);
        clientSecretEntity.setClientSecret(passwordEncoder.encode(rawSecret));
        clientSecretEntity.setClientSecretHint(hint);
        // client_secret_expires_at 默认为空，代表永不过期
        clientSecretEntity.setClientSecretExpiresAt(null);
        clientSecretEntity.setCreateTime(LocalDateTime.now());
        oAuth2ClientSecretService.save(clientSecretEntity);
    }
}
