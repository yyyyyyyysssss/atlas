package com.atlas.common.core.autoconfigure;

import com.atlas.common.core.api.feign.FeignConfiguration;
import com.atlas.common.core.api.notification.NotificationApi;
import com.atlas.common.core.api.user.UserApi;
import com.atlas.common.core.aspect.ControllerLogAspect;
import com.atlas.common.core.http.HttpClientConfiguration;
import com.atlas.common.core.http.factory.HttpClientFactory;
import com.atlas.common.core.json.JacksonConfiguration;
import com.atlas.common.core.utils.SpringUtils;
import com.atlas.common.core.web.client.factory.RestClientFactory;
import com.atlas.common.core.web.filter.UserContextFilter;
import com.atlas.common.core.web.exception.GlobalExceptionAdvice;
import com.atlas.common.core.web.filter.MDCTraceFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
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
    @Import({GlobalExceptionAdvice.class, FeignConfiguration.class, SpringUtils.class})
    public static class WebFeatureConfiguration {

        /**
         * 1. 日志链路追踪过滤器 (优先级最高)
         */
        @Bean
        public FilterRegistrationBean<MDCTraceFilter> mdcTraceFilterRegistration() {
            FilterRegistrationBean<MDCTraceFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new MDCTraceFilter());
            registration.addUrlPatterns("/*");
            // 设置为最高优先级，最先生成 TraceId
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
            return registration;
        }

        /**
         * 2. 用户上下文过滤器 (紧跟其后)
         */
        @Bean
        public FilterRegistrationBean<UserContextFilter> userContextFilterRegistration() {
            FilterRegistrationBean<UserContextFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new UserContextFilter());
            registration.addUrlPatterns("/*");
            // 顺序排在 MDC 之后，这样在 UserContextFilter 里的日志就能打印出 TraceId 了
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
            return registration;
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(
                prefix = "atlas.aspect.controller-log",
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
