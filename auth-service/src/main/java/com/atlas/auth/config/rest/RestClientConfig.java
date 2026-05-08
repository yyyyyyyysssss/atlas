package com.atlas.auth.config.rest;

import com.atlas.common.core.http.factory.HttpClientFactory;
import com.atlas.common.core.web.client.factory.RestClientFactory;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public RestClient localRestClient(HttpClientFactory httpClientFactory, RestClientFactory restClientFactory) {
        // 直接指向 localhost，绕过域名解析和网关
        return restClientFactory.create(
                httpClientFactory.create(),
                builder -> {
                    builder
                            .baseUrl("http://localhost:" + serverPort)
                            .build();
                }
        );
    }

}
