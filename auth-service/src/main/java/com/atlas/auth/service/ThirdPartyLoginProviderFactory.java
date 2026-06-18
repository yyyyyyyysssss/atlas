package com.atlas.auth.service;

import com.atlas.auth.domain.entity.SsoProvider;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.security.properties.SecurityProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
public class ThirdPartyLoginProviderFactory {

    private final Map<String, ThirdPartyLoginProvider> providers = new ConcurrentHashMap<>();

    private final List<ThirdPartyLoginProvider> providerList;

    private final SsoProviderService ssoProviderService;

    private final SecurityProperties securityProperties;

    private final AutowireCapableBeanFactory beanFactory;

    @PostConstruct
    public void init() {
        // 将手动编写并被 Spring 管理的 Bean 放入 Map
        providerList.forEach(p -> providers.put(p.getProviderName().toLowerCase(), p));

        // 从数据库获取所有开启的 SAML2 提供商
        List<SsoProvider> ssoProviders = ssoProviderService.listByProtocol(SsoProviderProtocol.SAML2);
        // 动态补全缺失的通用实现
        for (SsoProvider ssoProvider : ssoProviders) {
            String providerName = ssoProvider.getProvider().toLowerCase();
            // 检查已存在的实现类中是否包含该名称
            boolean exists = providerList.stream().anyMatch(p -> p.getProviderName().equalsIgnoreCase(providerName));
            if (!exists) {
                GenericSaml2LoginProvider genericSaml2LoginProvider = new GenericSaml2LoginProvider(providerName, ssoProviderService, securityProperties.getSaml2AuthUrl());
                // 让 Spring 完成该对象的依赖注入
                beanFactory.autowireBean(genericSaml2LoginProvider);

                providers.put(providerName, genericSaml2LoginProvider);
            }
        }
    }

    public ThirdPartyLoginProvider getProvider(String providerName) {
        ThirdPartyLoginProvider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            throw new RuntimeException("Unsupported login provider: " + providerName);
        }
        return provider;
    }

}
