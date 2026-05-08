package com.atlas.auth.service;

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
public class ThirdPartyLoginProviderFactory {

    private final Map<String, ThirdPartyLoginProvider> providers = new ConcurrentHashMap<>();

    public ThirdPartyLoginProviderFactory(List<ThirdPartyLoginProvider> providerList) {
        providerList.forEach(p -> providers.put(p.getProviderName().toLowerCase(), p));
    }


    public ThirdPartyLoginProvider getProvider(String providerName) {
        ThirdPartyLoginProvider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            throw new RuntimeException("Unsupported login provider: " + providerName);
        }
        return provider;
    }

}
