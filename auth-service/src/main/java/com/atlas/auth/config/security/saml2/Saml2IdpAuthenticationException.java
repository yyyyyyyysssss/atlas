package com.atlas.auth.config.security.saml2;

import lombok.Getter;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;

@Getter
public class Saml2IdpAuthenticationException extends Saml2AuthenticationException {

    private final AuthnRequest authnRequest;

    public Saml2IdpAuthenticationException(Saml2Error error) {
        this(error, null);
    }

    public Saml2IdpAuthenticationException(Saml2Error error, AuthnRequest authnRequest) {
        super(error);
        this.authnRequest = authnRequest;
    }
}
