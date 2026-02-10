package com.atlas.common.core.http;

import com.atlas.common.core.http.factory.HttpClientFactory;
import com.atlas.common.core.http.properties.HttpClientProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/10 17:31
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HttpClientProperties.class)
public class HttpClientConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HttpClientFactory httpClientFactory(HttpClientProperties properties) {
        return new HttpClientFactory(properties);
    }

}
