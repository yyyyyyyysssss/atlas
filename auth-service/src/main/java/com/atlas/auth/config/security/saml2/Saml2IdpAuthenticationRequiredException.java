package com.atlas.auth.config.security.saml2;

import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/12 9:00
 */
@Getter
public class Saml2IdpAuthenticationRequiredException extends AuthenticationException {

    private final String issuer;

    private final String requestId;

    public Saml2IdpAuthenticationRequiredException(String issuer, String requestId) {
        super("User authentication required");
        this.requestId = requestId;
        this.issuer = issuer;
    }
}
