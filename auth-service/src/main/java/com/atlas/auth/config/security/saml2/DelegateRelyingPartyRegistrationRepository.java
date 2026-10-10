package com.atlas.auth.config.security.saml2;

import com.atlas.auth.domain.dto.Saml2ProviderSettings;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.service.SsoProviderService;
import com.atlas.common.security.properties.SecurityProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/26 17:24
 */
@Component("relyingPartyRegistrationRepository")
@Slf4j
public class DelegateRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

    private final SsoProviderService ssoProviderService;

    private final SecurityProperties securityProperties;

    private final Saml2X509Credential signingCredential;

    private final Cache<String, Optional<RelyingPartyRegistration>> registrationCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();

    public DelegateRelyingPartyRegistrationRepository(SsoProviderService ssoProviderService, SecurityProperties securityProperties, Saml2SigningCredentialLoader saml2SigningCredentialLoader) {
        this.ssoProviderService = ssoProviderService;
        this.securityProperties = securityProperties;

        SecurityProperties.SigningConfig config = securityProperties.getSaml2().getSp().getSigning();
        String keyStoreLocation = config.getKeyStore();
        String keyStorePassword = config.getKeyStorePassword();
        String keyAlias = config.getKeyAlias();

        Saml2SigningCredentialLoader.Saml2SigningCredential saml2SigningCredential = saml2SigningCredentialLoader.load(keyStoreLocation, keyStorePassword, keyAlias);
        this.signingCredential = saml2SigningCredential.toSpringCredential();
    }

    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        if (registrationId == null) {
            return null;
        }
        return Objects.requireNonNull(registrationCache.get(registrationId, this::loadByProvider)).orElse(null);
    }

    private Optional<RelyingPartyRegistration> loadByProvider(String provider){
        log.info("[SAML2] 正在动态加载提供商 [{}] 的配置...", provider);
        Saml2ProviderSettings settings = ssoProviderService.getSettings(provider, SsoProviderProtocol.SAML2);
        if (settings == null) {
            log.warn("[SAML2] 未在数据库中找到提供商 [{}] 的有效配置", provider);
            return Optional.empty();
        }
        try {
            // 优先使用 SAML Metadata
            if (settings.metadataUrl() != null && !settings.metadataUrl().isBlank()) {
                log.info("[SAML2] 提供商 [{}] 使用 Metadata 加载模式，metadataUrl={}", provider, settings.metadataUrl());
                RelyingPartyRegistration relyingPartyRegistration = loadByMetadata(provider, settings);
                return Optional.of(relyingPartyRegistration);
            }
            log.info("[SAML2] 提供商 [{}] 使用手工配置加载模式", provider);
            RelyingPartyRegistration relyingPartyRegistration = loadByConfiguration(provider, settings);
            return Optional.of(relyingPartyRegistration);
        } catch (Exception e) {
            log.error("[SAML2] 动态加载提供商 [" + provider + "] 配置失败!", e);
            return Optional.empty();
        }
    }

    private RelyingPartyRegistration loadByMetadata(String provider, Saml2ProviderSettings settings) {
        return RelyingPartyRegistrations
                .fromMetadataLocation(settings.metadataUrl())
                .registrationId(provider)
                .entityId(settings.entityId())
                .assertionConsumerServiceLocation(securityProperties.getIssuerUrl() + settings.acs().location())
                .assertionConsumerServiceBinding(settings.acs().binding())
                .signingX509Credentials(signingX509Credentials -> signingX509Credentials.add(signingCredential))
                .build();
    }

    private RelyingPartyRegistration loadByConfiguration(String provider, Saml2ProviderSettings settings) throws Exception {
        // 提取并转换证书凭证
        List<Saml2X509Credential> credentials = new ArrayList<>();
        if (settings.assertingparty() != null && settings.assertingparty().verification() != null) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            for (Saml2ProviderSettings.Credential cred : settings.assertingparty().verification().credentials()) {
                // 通过 Resource 载入输入流并解析 X509 证书
                try (InputStream is = cred.certificateLocation().getInputStream()) {
                    X509Certificate certificate = (X509Certificate) factory.generateCertificate(is);
                    credentials.add(Saml2X509Credential.verification(certificate));
                }
            }
        }
        // 映射构建 RelyingPartyRegistration 实例
        return RelyingPartyRegistration
                .withRegistrationId(provider)
                .entityId(settings.entityId())
                .assertionConsumerServiceLocation(securityProperties.getIssuerUrl() + settings.acs().location())
                .assertionConsumerServiceBinding(settings.acs().binding())
                // 使用官方推荐的 assertingPartyMetadata
                .assertingPartyMetadata(party -> {
                    party.entityId(Objects.requireNonNull(settings.assertingparty()).entityId())
                            .singleSignOnServiceLocation(settings.assertingparty().singlesignon().url())
                            .singleSignOnServiceBinding(settings.assertingparty().singlesignon().binding())
                            .wantAuthnRequestsSigned(Objects.requireNonNull(settings.assertingparty()).singlesignon().signRequest())
                            .verificationX509Credentials(c -> c.addAll(credentials));
                })
                .signingX509Credentials(signingX509Credentials -> signingX509Credentials.add(signingCredential))
                .build();
    }

    public void clearCache(String provider) {
        if (provider != null) {
            registrationCache.invalidate(provider);
            log.info("[SAML2] 已清除提供商 [{}] 的本地缓存，下次登录将实时加载新配置", provider);
        } else {
            registrationCache.invalidateAll();
            log.info("[SAML2] 已清空全部 SAML2 提供商的本地缓存");
        }
    }
}
