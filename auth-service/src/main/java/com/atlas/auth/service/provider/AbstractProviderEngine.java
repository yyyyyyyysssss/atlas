package com.atlas.auth.service.provider;

import com.atlas.auth.domain.entity.SsoProvider;
import com.atlas.auth.service.SsoProviderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

/**
 * @Description
 * @Author ys
 * @Date 2026/11/4 15:02
 */
@Slf4j
public abstract class AbstractProviderEngine {

    @Resource
    private RestClient proxyRestClient;

    @Resource
    private RestClient defaultRestClient;

    @Resource
    private SsoProviderService ssoProviderService;

    protected RestClient getClient(String provider) {
        SsoProvider ssoProvider = ssoProviderService.getProvider(provider);
        if (ssoProvider == null) {
            log.warn("未找到提供商配置: {}, 使用默认客户端", provider);
        }
        return (ssoProvider != null && ssoProvider.getUseProxy()) ? proxyRestClient : defaultRestClient;
    }

}
