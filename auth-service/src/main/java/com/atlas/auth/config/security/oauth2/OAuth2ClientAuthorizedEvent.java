package com.atlas.auth.config.security.oauth2;

import org.springframework.context.ApplicationEvent;

/**
 * @Description
 * @Author ys
 * @Date 2026/7/7 17:22
 */
public class OAuth2ClientAuthorizedEvent extends ApplicationEvent {

    private final String clientId;

    private final String username;

    public OAuth2ClientAuthorizedEvent(Object source, String clientId, String username) {
        super(source);
        this.clientId = clientId;
        this.username = username;
    }

    public String getClientId() {
        return clientId;
    }

    public String getUsername() {
        return username;
    }
}
