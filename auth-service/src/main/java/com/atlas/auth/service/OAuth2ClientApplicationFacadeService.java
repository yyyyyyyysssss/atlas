package com.atlas.auth.service;


import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.domain.dto.ApplicationClientSaveDTO;
import com.atlas.auth.domain.dto.OAuth2ClientApplicationQueryDTO;
import com.atlas.auth.domain.dto.OAuth2ClientApplicationSaveDTO;
import com.atlas.auth.domain.entity.Application;
import com.atlas.auth.domain.entity.ApplicationClient;
import com.atlas.auth.domain.entity.OAuth2ClientSecret;
import com.atlas.auth.domain.vo.OAuth2ClientApplicationCreateVO;
import com.atlas.auth.domain.vo.OAuth2ClientApplicationVO;
import com.atlas.auth.domain.vo.ProjectVO;
import com.atlas.auth.enums.ClientProtocol;
import com.atlas.auth.event.AuditLogEvent;
import com.atlas.auth.mapper.OAuth2RegisteredClientMapper;
import com.atlas.common.core.context.UserContext;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.common.crypto.random.SecureRandomUtils;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
public class OAuth2ClientApplicationFacadeService extends AbstractClientApplicationFacadeService {

    private final ApplicationService applicationService;

    private final ApplicationClientService applicationClientService;

    private final RegisteredClientRepository registeredClientRepository;

    private final OAuth2RegisteredClientMapper oAuth2RegisteredClientMapper;

    private final OAuth2ClientSecretService oAuth2ClientSecretService;

    private final PasswordEncoder passwordEncoder;

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 保存应用（兼容创建和修改）
     */
    @Transactional(rollbackFor = Exception.class)
    public OAuth2ClientApplicationCreateVO save(String projectCode, Long applicationId, OAuth2ClientApplicationSaveDTO saveDTO) {
        validateProjectAndApplication(projectCode, applicationId);
        if (saveDTO.id() == null) {
            return createApplication(applicationId, saveDTO);
        } else {
            updateApplication(applicationId, saveDTO);
            return null;
        }
    }

    public PageInfo<OAuth2ClientApplicationVO> getPage(String projectCode, Long applicationId, OAuth2ClientApplicationQueryDTO queryDTO) {
        // 前置校验
        validateProjectAndApplication(projectCode, applicationId);

        Integer pageNum = queryDTO.getPageNum();
        Integer pageSize = queryDTO.getPageSize();
        PageInfo<ApplicationClient> clientPageInfo = applicationClientService.getPage(applicationId, ClientProtocol.OAUTH2, pageNum, pageSize);

        return clientPageInfo.convert(OAuth2ClientApplicationVO::of);
    }

    public OAuth2ClientApplicationVO getOAuth2ClientDetail(String projectCode, Long applicationId, Long id) {
        // 前置校验
        validateProjectAndApplication(projectCode, applicationId);

        ApplicationClient applicationClient = applicationClientService.getById(id);

        RegisteredClient registeredClient = registeredClientRepository.findById(applicationClient.getRegisteredClientId());
        if (registeredClient == null) {
            throw new BusinessException("应用关联的oauth2客户端数据丢失，请检查数据一致性");
        }
        List<OAuth2ClientSecret> oAuth2ClientSecrets = oAuth2ClientSecretService.listValidSecretsByRegisteredClientId(registeredClient.getId());
        return OAuth2ClientApplicationVO.of(applicationClient, registeredClient, oAuth2ClientSecrets);
    }

    @Transactional
    public void deleteById(String projectCode,Long applicationId, Long id) {
        validateProjectAndApplication(projectCode, applicationId);

        log.info("正在删除 OAuth2 客户端，ID: {}", id);
        ApplicationClient applicationClient = applicationClientService.getById(id);
        // 删除oauth2客户端密钥信息
        oAuth2ClientSecretService.removeByRegisteredClientId(applicationClient.getRegisteredClientId());
        // 删除oauth2客户端对应的所有token
        oAuth2RegisteredClientMapper.deleteAllClientToken(applicationClient.getRegisteredClientId());
        // 删除oauth2标准客户端信息
        oAuth2RegisteredClientMapper.deleteById(applicationClient.getRegisteredClientId());

        // 删除客户端应用主表信息
        boolean delFlag = applicationClientService.removeById(id);
        if (!delFlag) {
            log.error("删除 OAuth2 客户端主表记录失败，ID: {}", id);
            throw new AuthServiceException("删除OAuth2 客户端失败，请稍后重试");
        }

        log.info("OAuth2 客户端删除成功，ID: {}", id);

        // 发布审计日志
        eventPublisher.publishEvent(new AuditLogEvent(UserContext.getRequiredUserId(), "删除OAuth2客户端", "project:application:client:oauth2"));
    }

    @Transactional(rollbackFor = Exception.class)
    public OAuth2ClientApplicationCreateVO addClientSecret(String projectCode, Long applicationId, Long id) {
        validateProjectAndApplication(projectCode, applicationId);

        log.info("正在为 OAuth2 客户端生成新密钥，ID: {}", applicationId);

        ApplicationClient applicationClient = applicationClientService.getById(id);

        long countValidSecrets = oAuth2ClientSecretService.countValidSecrets(applicationClient.getRegisteredClientId());
        if (countValidSecrets >= 2) {
            throw new BusinessException("最多同时存在 2 个有效密钥，请先删除旧密钥后再生成新密钥");
        }
        RegisteredClient registeredClient = registeredClientRepository.findById(applicationClient.getRegisteredClientId());
        if (registeredClient == null) {
            throw new BusinessException("应用关联的oauth2客户端数据丢失，请检查数据一致性");
        }
        String rawSecret = SecureRandomUtils.generate(32);
        // 保存密钥
        saveClientSecret(applicationClient, rawSecret);

        // 发布审计日志
        eventPublisher.publishEvent(new AuditLogEvent(UserContext.getRequiredUserId(), "新增OAuth2客户端密钥", "project:application:client:oauth2:secret"));

        return new OAuth2ClientApplicationCreateVO(applicationClient.getId(), registeredClient.getClientId(), rawSecret);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteClientSecret(String projectCode, Long applicationId, Long id, Long clientSecretId) {
        validateProjectAndApplication(projectCode, applicationId);

        log.info("正在删除 OAuth2 客户端密钥，ID: {}", clientSecretId);
        OAuth2ClientSecret targetSecret = oAuth2ClientSecretService.getById(clientSecretId);
        if (targetSecret == null) {
            throw new AuthServiceException("该密钥不存在或已被删除");
        }
        if (!Objects.equals(targetSecret.getApplicationClientId(), id)) {
            throw new AuthServiceException("该密钥应用信息不匹配");
        }

        String registeredClientId = targetSecret.getRegisteredClientId();
        List<OAuth2ClientSecret> validSecrets = oAuth2ClientSecretService.listValidSecretsByRegisteredClientId(registeredClientId);
        if (CollectionUtils.isEmpty(validSecrets) || validSecrets.size() == 1) {
            throw new BusinessException("至少保留 1 个有效密钥，无法继续删除");
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

        log.info("OAuth2 客户端密钥删除成功，ID: {}，已同步剩余密钥给核心框架", clientSecretId);

        // 发布审计日志
        eventPublisher.publishEvent(new AuditLogEvent(UserContext.getRequiredUserId(), "删除OAuth2客户端密钥", "project:application:client:oauth2:secret"));
    }

    private OAuth2ClientApplicationCreateVO createApplication(Long applicationId, OAuth2ClientApplicationSaveDTO saveDTO) {
        log.info("正在创建 OAuth2 客户端，名称: {}", saveDTO.clientName());
        // oauth2_registered_client 物理主键
        String registeredClientId = UUID.randomUUID().toString().replace("-", "");
        // 暴露给用户的 clientId
        String clientId = SecureRandomUtils.generate(20);
        // 暴露给用户的 clientSecret
        String rawSecret = SecureRandomUtils.generate(32);
        // 加密后的密钥
        String clientSecret = passwordEncoder.encode(rawSecret);
        RegisteredClient.Builder clientBuilder = RegisteredClient.withId(registeredClientId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientName(saveDTO.clientName())
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
        RegisteredClient registeredClient = clientBuilder.build();
        registeredClientRepository.save(registeredClient);

        // 保存 (application_client)
        ApplicationClientSaveDTO clientSaveDTO = ApplicationClientSaveDTO.of(applicationId, saveDTO, registeredClient);
        ApplicationClient applicationClient = applicationClientService.saveInfo(clientSaveDTO);

        // 保存密钥
        saveClientSecret(applicationClient, rawSecret);

        log.info("OAuth2 应用创建成功, id: {}, clientId: {}", applicationClient.getId(), clientId);

        // 发布审计日志
        eventPublisher.publishEvent(new AuditLogEvent(UserContext.getRequiredUserId(), "创建OAuth2客户端", "project:application:client:oauth2"));

        return new OAuth2ClientApplicationCreateVO(applicationClient.getId(), clientId, rawSecret);
    }

    private void updateApplication(Long applicationId, OAuth2ClientApplicationSaveDTO saveDTO) {
        log.info("正在修改 OAuth2 客户端，ID: {}", applicationId);

        ApplicationClient applicationClient = applicationClientService.getById(saveDTO.id());

        RegisteredClient registeredClient = registeredClientRepository.findById(applicationClient.getRegisteredClientId());
        if (registeredClient == null) {
            throw new AuthServiceException("客户端[" + applicationClient.getClientName() + "]关联的OAuth2配置数据丢失，请检查数据一致性");
        }
        List<OAuth2ClientSecret> oAuth2ClientSecrets = oAuth2ClientSecretService.listValidSecretsByRegisteredClientId(applicationClient.getRegisteredClientId());
        if (CollectionUtils.isEmpty(oAuth2ClientSecrets)) {
            throw new BusinessException("当前客户端应用密钥为空");
        }
        RegisteredClient updatedClient = RegisteredClient.from(registeredClient)
                .clientName(saveDTO.clientName())
                .clientSecret(oAuth2ClientSecrets.getFirst().getClientSecret())
                // 清理旧的回调和范围，重新注入新配置
                .redirectUris(uris -> {
                    uris.clear();
                    if (saveDTO.redirectUri() != null) uris.addAll(saveDTO.redirectUri());
                })
                .scopes(scopes -> {
                    scopes.clear();
                    if (saveDTO.scopes() != null) scopes.addAll(saveDTO.scopes());
                })
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

        // 同步更新 application_client
        ApplicationClientSaveDTO clientSaveDTO = ApplicationClientSaveDTO.of(applicationId, saveDTO, registeredClient);
        applicationClientService.saveInfo(clientSaveDTO);

        log.info("OAuth2 客户端配置更新成功, id: {}, clientName: {}", applicationClient.getId(), applicationClient.getClientName());

        // 发布审计日志
        eventPublisher.publishEvent(new AuditLogEvent(UserContext.getRequiredUserId(), "修改OAuth2客户端", "project:application:client:oauth2"));
    }

    private void saveClientSecret(ApplicationClient applicationClient, String rawSecret) {
        // 保存密钥
        String hint = rawSecret.length() > 4 ? rawSecret.substring(rawSecret.length() - 4) : rawSecret;
        OAuth2ClientSecret clientSecretEntity = new OAuth2ClientSecret();
        clientSecretEntity.setId(IdGen.genId());
        clientSecretEntity.setApplicationClientId(applicationClient.getId());
        clientSecretEntity.setRegisteredClientId(applicationClient.getRegisteredClientId());
        clientSecretEntity.setClientSecret(passwordEncoder.encode(rawSecret));
        clientSecretEntity.setClientSecretHint(hint);
        // client_secret_expires_at 默认为空，代表永不过期
        clientSecretEntity.setClientSecretExpiresAt(null);
        clientSecretEntity.setCreateTime(LocalDateTime.now());
        oAuth2ClientSecretService.save(clientSecretEntity);
    }

    /**
     * 校验项目是否存在，以及应用是否属于该项目。
     *
     * @param projectCode   项目编码
     * @param applicationId 应用ID
     */
    private Application validateProjectAndApplication(String projectCode, Long applicationId) {
        ProjectVO project = projectService.getRequiredByCode(projectCode);
        return applicationService.getRequiredByIdAndProjectId(applicationId, project.id());
    }

}
