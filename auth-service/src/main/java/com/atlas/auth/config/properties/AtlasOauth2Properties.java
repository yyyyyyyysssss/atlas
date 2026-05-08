package com.atlas.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/8 13:31
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.other-login.atlas")
public class AtlasOauth2Properties {

    private String clientName;

    private String clientId;

    private String clientSecret;

    private String redirectUrl;

    private String scope;

    private String authorizeCodeUrl;

    private String tokenUrl;

    private String userInfoUrl;

}
