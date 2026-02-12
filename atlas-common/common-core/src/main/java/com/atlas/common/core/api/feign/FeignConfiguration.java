package com.atlas.common.core.api.feign;

import com.atlas.common.core.http.factory.HttpClientFactory;
import feign.Client;
import feign.hc5.ApacheHttp5Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/12 14:38
 */
@Configuration
@ConditionalOnClass(ApacheHttp5Client.class)
public class FeignConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Client feignClient(HttpClientFactory httpClientFactory) {

        return new ApacheHttp5Client(httpClientFactory.getHttpClient());
    }

}
