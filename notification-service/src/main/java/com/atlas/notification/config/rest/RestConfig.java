package com.atlas.notification.config.rest;

import com.atlas.common.core.http.factory.HttpClientFactory;
import com.atlas.common.core.web.client.factory.RestClientFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * @Description
 * @Author ys
 * @Date 2024/8/2 21:13
 */
@Getter
@Setter
@Configuration
@Slf4j
public class RestConfig {

    @Bean
    public RestClient defaultRestClient(HttpClientFactory httpClientFactory, RestClientFactory restClientFactory){
        return restClientFactory.create(
                httpClientFactory.create(),
                builder -> {}
        );
    }

}
