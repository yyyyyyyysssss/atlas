package com.atlas.auth.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author ys
 * @Date 2023/9/18 15:42
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "security.oauth2.login.providers.github")
@Component
public class GithubOauth2Properties {

   private String clientName;

   private String clientId;

   private String clientSecret;

   private String redirectUrl;

   private String scope;

   private String authorizeCodeEndpoint;

   private String tokenEndpoint;

   private String userInfoEndpoint;

   private String userEmailsEndpoint;
}
