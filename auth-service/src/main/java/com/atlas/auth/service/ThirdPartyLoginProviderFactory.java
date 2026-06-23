package com.atlas.auth.service;

import com.atlas.auth.domain.entity.SsoProvider;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.security.properties.SecurityProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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

    @PostConstruct
    public void init() {
        // 将手动编写并被 Spring 管理的 Bean 放入 Map
        providerList.forEach(p -> {
            // 厂商名 + 下划线 + 协议名
            String uniqueKey = p.getProviderName().toLowerCase() + "_" + p.protocol().name().toLowerCase();
            providers.put(uniqueKey, p);
        });

        // 动态注册 SAML2 提供商
        registerDynamicProviders(SsoProviderProtocol.SAML2, name -> new GenericSaml2LoginProvider(name, securityProperties.getSaml2AuthUrl()));

        // 动态注册 OIDC 提供商
        registerDynamicProviders(SsoProviderProtocol.OIDC, name -> new GenericOidcLoginProvider(name, oidcProviderEngine));
    }

    private void registerDynamicProviders(SsoProviderProtocol protocol, Function<String, ThirdPartyLoginProvider> providerCreator){
        String protocolSuffix = protocol.name().toLowerCase();
        // 从数据库获取该协议下所有开启的提供商配置
        List<SsoProvider> ssoProviders = ssoProviderService.listByProtocol(protocol);
        for (SsoProvider ssoProvider : ssoProviders) {
            String providerName = ssoProvider.getProvider().toLowerCase();
            String uniqueKey = providerName + "_" + protocolSuffix;
            // 手写/自定义优先原则，不覆盖已存在的特殊实现
            if (!providers.containsKey(uniqueKey)) {
                // 通过回调外界传入的 Lambda 表达式，动态解耦具体类的实例化逻辑
                ThirdPartyLoginProvider dynamicProvider = providerCreator.apply(providerName);
                // 让 Spring 完成该动态对象的依赖注入
                beanFactory.autowireBean(dynamicProvider);

                providers.put(uniqueKey, dynamicProvider);

                log.info("三方登录通用注册器动态注入 [{}] 提供商实例 Bean: [{}]", protocol.name(), uniqueKey);
            }
        }
    }

    public ThirdPartyLoginProvider getProvider(String providerName) {

        return getProvider(providerName, SsoProviderProtocol.OAUTH2);
    }

    public ThirdPartyLoginProvider getProvider(String providerName, SsoProviderProtocol protocol) {
        String uniqueKey = providerName.toLowerCase() + "_" + protocol.name().toLowerCase();
        ThirdPartyLoginProvider provider = providers.get(uniqueKey);
        if (provider == null) {
            throw new BusinessException("系统未启用或不支持该三方登录: [" + providerName + "] 协议: [" + protocol.name() + "]");
        }
        return provider;
    }

}
