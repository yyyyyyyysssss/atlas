package com.atlas.user.config.rest;

import com.atlas.common.core.http.factory.HttpClientFactory;
import com.atlas.common.core.web.client.factory.RestClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/8 14:40
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient proxyRestClient(HttpClientFactory httpClientFactory, RestClientFactory restClientFactory) {
        return restClientFactory.create(
                httpClientFactory.create(true),
                builder -> {}
        );
    }

}
