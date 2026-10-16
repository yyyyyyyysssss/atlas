package com.atlas.auth.service;

import com.atlas.auth.config.security.ldap.LdapTemplateFactory;
import com.atlas.auth.domain.entity.SsoProvider;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.security.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/8 14:59
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThirdPartyLoginProviderFactory {

    private final Map<String, ThirdPartyLoginProvider> providers = new ConcurrentHashMap<>();

    private final List<ThirdPartyLoginProvider> providerList;

    private final SsoProviderService ssoProviderService;

    private final SecurityProperties securityProperties;

    private final AutowireCapableBeanFactory beanFactory;

    private final OidcProviderEngine oidcProviderEngine;

    private final LdapTemplateFactory ldapTemplateFactory;

    public ThirdPartyLoginProvider getProvider(String providerName) {

        return getProvider(providerName, SsoProviderProtocol.OAUTH2);
    }

    public ThirdPartyLoginProvider getProvider(String providerName, SsoProviderProtocol protocol) {
        String uniqueKey = buildKey(providerName, protocol);
        ThirdPartyLoginProvider provider = providers.get(uniqueKey);
        if (provider != null) {
            return provider;
        }
        // 从 Spring Bean 中查找手写 Provider
        provider = providerList.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName) && p.protocol() == protocol)
                .findFirst()
                .orElse(null);
        if (provider != null) {
            providers.putIfAbsent(uniqueKey, provider);
            return provider;
        }
        // 动态创建
        provider = createProvider(providerName, protocol);

        providers.putIfAbsent(uniqueKey, provider);

        return provider;
    }

    public ThirdPartyLoginProvider createProvider(String providerName, SsoProviderProtocol protocol){
        SsoProvider ssoProvider = ssoProviderService.getProvider(providerName);
        if (ssoProvider == null || !ssoProvider.getEnabled()) {
            throw new BusinessException("系统未启用或不支持该三方登录: [" + providerName + "] 协议: [" + protocol.name() + "]");
        }
        ThirdPartyLoginProvider provider;
        switch (protocol){
            case OIDC ->  provider = new GenericOidcLoginProvider(providerName, oidcProviderEngine);
            case SAML2 -> provider = new GenericSaml2LoginProvider(providerName, securityProperties.getSaml2().getSp().getAuthenticateUrl());
            case LDAP -> provider = new GenericLdapLoginProvider(providerName, ldapTemplateFactory);
            default -> throw new BusinessException("不支持动态创建的登录协议: " + protocol.name());
        }
        beanFactory.autowireBean(provider);
        return provider;
    }


    private String buildKey(String providerName, SsoProviderProtocol protocol) {

        return providerName.toLowerCase() + "_" + protocol.name().toLowerCase();
    }
}
