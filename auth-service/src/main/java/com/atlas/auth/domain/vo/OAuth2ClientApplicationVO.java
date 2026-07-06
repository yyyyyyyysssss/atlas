package com.atlas.auth.domain.vo;

import com.atlas.auth.domain.entity.OAuth2ClientApplication;
import com.atlas.auth.domain.entity.OAuth2ClientSecret;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.LocalDateTime;
import java.util.List;

public record OAuth2ClientApplicationVO(
        Long id,
        String clientId, // 只读，回显给用户看
        String applicationName,
        String logoUrl,
        String homePageUrl,
        List<String> redirectUri,
        List<String> scopes,
        Boolean allowDeviceFlow,
        String description,
        LocalDateTime createTime,
        LocalDateTime lastUsedTime,
        List<ClientSecretInfoVO> clientSecrets
) {

    public static OAuth2ClientApplicationVO of(OAuth2ClientApplication application){
        return new OAuth2ClientApplicationVO(
                application.getId(),
                application.getClientId(),
                application.getApplicationName(),
                application.getLogoUrl(),
                application.getHomePageUrl(),
                null,
                null,
                null,
                application.getDescription(),
                application.getCreateTime(),
                application.getLastUsedTime(),
                null
        );
    }

    public static OAuth2ClientApplicationVO of(OAuth2ClientApplication application, RegisteredClient registeredClient, List<OAuth2ClientSecret> oAuth2ClientSecrets){
        List<ClientSecretInfoVO> secretInfoVOs = null;
        if (oAuth2ClientSecrets != null) {
            secretInfoVOs = oAuth2ClientSecrets.stream()
                    .map(s -> new ClientSecretInfoVO(s.getId(), s.getClientSecretHint(), s.getClientSecretExpiresAt(), s.getCreateTime()))
                    .toList();
        }
        return new OAuth2ClientApplicationVO(
                application.getId(),
                application.getClientId(),
                application.getApplicationName(),
                application.getLogoUrl(),
                application.getHomePageUrl(),
                registeredClient.getRedirectUris().stream().toList(),
                registeredClient.getScopes().stream().toList(),
                registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.DEVICE_CODE),
                application.getDescription(),
                application.getCreateTime(),
                application.getLastUsedTime(),
                secretInfoVOs
        );
    }


    public record ClientSecretInfoVO(
            Long id,
            String clientSecretHint,
            LocalDateTime clientSecretExpiresAt,
            LocalDateTime createTime
    ) {}

}
