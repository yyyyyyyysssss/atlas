package com.atlas.common.api.autoconfigure;


import com.atlas.common.api.UserApi;
import com.atlas.common.core.autoconfigure.AtlasCoreAutoConfiguration;
import com.atlas.common.core.http.factory.HttpClientFactory;
import com.atlas.common.core.http.factory.RestClientFactory;
import com.atlas.common.core.http.support.HttpInterfaceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration(after = AtlasCoreAutoConfiguration.class)
public class UserClientAutoConfiguration {


    @Value("${atlas.api.user.base-url:http://user-service}")
    private String baseUrl;

    @Bean
    @ConditionalOnBean({HttpClientFactory.class, RestClientFactory.class})
    @ConditionalOnMissingBean
    public UserApi userApi(HttpClientFactory httpClientFactory, RestClientFactory restClientFactory) {
        log.info("Initialising Declarative Http Interface: UserApi, baseUrl: {}", baseUrl);

        return HttpInterfaceUtils.createClient(
                UserApi.class,
                baseUrl,
                httpClientFactory,
                restClientFactory
        );
    }

}
