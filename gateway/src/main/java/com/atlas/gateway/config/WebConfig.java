package com.atlas.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Description
 * @Author ys
 * @Date 2024/8/8 16:53
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${security.uiUrl}")
    private String uiUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(uiUrl)
                .allowCredentials(true)
                .allowedHeaders("*")
                .allowedMethods("GET","POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    }
}
