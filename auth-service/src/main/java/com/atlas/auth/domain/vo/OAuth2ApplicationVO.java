package com.atlas.auth.domain.vo;

import com.atlas.auth.domain.entity.OAuth2Application;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.LocalDateTime;
import java.util.List;

public record OAuth2ApplicationVO(
        Long id,
        String clientId, // 只读，回显给用户看
        String applicationName,
        String logoUrl,
        String homePageUrl,
        List<String> redirectUri,
        List<String> scopes,
        Boolean allowDeviceFlow,
        String description,
        LocalDateTime createTime
) {

    public static OAuth2ApplicationVO of(OAuth2Application application){
        return new OAuth2ApplicationVO(
                application.getId(),
                application.getClientId(),
                application.getApplicationName(),
                application.getLogoUrl(),
                application.getHomePageUrl(),
                null,
                null,
                null,
                application.getDescription(),
                application.getCreateTime()
        );
    }

    public static OAuth2ApplicationVO of(OAuth2Application application, RegisteredClient registeredClient){

        return new OAuth2ApplicationVO(
                application.getId(),
                application.getClientId(),
                application.getApplicationName(),
                application.getLogoUrl(),
                application.getHomePageUrl(),
                registeredClient.getRedirectUris().stream().toList(),
                registeredClient.getScopes().stream().toList(),
                registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.DEVICE_CODE),
                application.getDescription(),
                application.getCreateTime()
        );
    }

}
