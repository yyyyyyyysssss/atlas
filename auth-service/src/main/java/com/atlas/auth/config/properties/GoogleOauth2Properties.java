package com.atlas.auth.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author ys
 * @Date 2023/9/20 20:12
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "security.oauth2.login.providers.google")
@Component
public class GoogleOauth2Properties {

    private String clientName;

    private String clientId;

    private String clientSecret;

    private String scope;

    private String authorizeCodeEndpoint;

    private String tokenEndpoint;

    private String redirectUrl;

    private String userInfoEndpoint;
}
