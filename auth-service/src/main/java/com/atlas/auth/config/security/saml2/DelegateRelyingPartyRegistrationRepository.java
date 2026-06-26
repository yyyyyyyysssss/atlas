package com.atlas.auth.config.security.saml2;

import com.atlas.auth.domain.dto.Saml2ProviderSettings;
import com.atlas.auth.domain.entity.SsoProvider;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.service.SsoProviderService;
import com.atlas.security.properties.SecurityProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.IterableRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/26 17:24
 */
@Component("relyingPartyRegistrationRepository")
@Slf4j
public class DelegateRelyingPartyRegistrationRepository implements IterableRelyingPartyRegistrationRepository, RelyingPartyRegistrationRepository {

    private final SsoProviderService ssoProviderService;

    private final SecurityProperties securityProperties;

    private final Cache<String, Optional<RelyingPartyRegistration>> registrationCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();

    public DelegateRelyingPartyRegistrationRepository(SsoProviderService ssoProviderService, SecurityProperties securityProperties) {
        this.ssoProviderService = ssoProviderService;
        this.securityProperties = securityProperties;
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
            RelyingPartyRegistration registration = RelyingPartyRegistration
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
                    .build();
            return Optional.of(registration);
        } catch (Exception e) {
            log.error("[SAML2] 动态加载提供商 [" + provider + "] 配置失败!", e);
            return Optional.empty();
        }
    }

    @NotNull
    @Override
    public Iterator<RelyingPartyRegistration> iterator() {
        List<SsoProvider> ssoProviders = ssoProviderService.listByProtocol(SsoProviderProtocol.SAML2);
        if (ssoProviders == null) {
            return Collections.emptyIterator();
        }
        return ssoProviders.stream()
                .map(SsoProvider::getProvider)
                .map(this::findByRegistrationId)
                .filter(Objects::nonNull)
                .toList()
                .iterator();
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
