package com.atlas.common.core.autoconfigure;

import com.atlas.common.core.api.feign.FeignConfiguration;
import com.atlas.common.core.api.notification.NotificationApi;
import com.atlas.common.core.api.user.UserApi;
import com.atlas.common.core.aspect.ControllerLogAspect;
import com.atlas.common.core.http.HttpClientConfiguration;
import com.atlas.common.core.http.factory.HttpClientFactory;
import com.atlas.common.core.json.JacksonConfiguration;
import com.atlas.common.core.web.client.factory.RestClientFactory;
import com.atlas.common.core.web.handler.GlobalExceptionHandler;
import com.atlas.common.core.web.filter.MDCTraceFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/10 15:52
 */
@AutoConfiguration
@Import({
        HttpClientConfiguration.class,
        JacksonConfiguration.class
})
public class AtlasCoreAutoConfiguration {


    /**
     * Web 环境特有组件 (仅在 Web 服务中激活)
     */
    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @Import({GlobalExceptionHandler.class, FeignConfiguration.class, MDCTraceFilter.class})
    public static class WebFeatureConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(
                prefix = "aspect.controller-log",
                name = "enabled",
                havingValue = "true",
                matchIfMissing = true
        )
        public ControllerLogAspect controllerLogAspect(ObjectMapper objectMapper) {

            return new ControllerLogAspect(objectMapper);
        }

        @Bean
        @ConditionalOnMissingBean
        public RestClientFactory restClientFactory(ObjectMapper objectMapper) {
            return new RestClientFactory(objectMapper);
        }

        @Bean
        @ConditionalOnMissingBean
        public RestClient defaultRestClient(HttpClientFactory httpClientFactory, RestClientFactory restClientFactory) {
            return restClientFactory.create(httpClientFactory.create(), builder -> {
            });
        }


        @Bean
        @ConditionalOnProperty(prefix = "atlas.notification", name = "server-url")
        public NotificationApi notificationApi(@Value("${atlas.notification.server-url}") String url, RestClientFactory restClientFactory, HttpClientFactory httpClientFactory) {

            return createProxy(NotificationApi.class, url, restClientFactory, httpClientFactory);
        }

        @Bean
        @ConditionalOnProperty(prefix = "atlas.user", name = "server-url")
        public UserApi userApi(@Value("${atlas.user.server-url}") String url, RestClientFactory restClientFactory, HttpClientFactory httpClientFactory) {

            return createProxy(UserApi.class, url, restClientFactory, httpClientFactory);
        }

        private <T> T createProxy(Class<T> clazz, String url, RestClientFactory factory, HttpClientFactory httpFactory) {
            RestClient restClient = factory.create(httpFactory.create(), url, builder -> {
            });
            return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                    .build()
                    .createClient(clazz);
        }

    }


}
