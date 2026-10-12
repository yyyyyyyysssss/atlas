package com.atlas.auth.config.security.saml2;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/10 17:51
 */
public class Saml2IdpAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;

    private Object credentials;

    private final Saml2IdpRequestContext saml2IdpRequestContext;

    public Saml2IdpAuthenticationToken(Saml2IdpRequestContext requestContext){
        super(null);
        this.principal = null;
        this.credentials = null;
        this.saml2IdpRequestContext = requestContext;
    }

    public Saml2IdpAuthenticationToken(Object principal, Saml2IdpRequestContext requestContext, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = null;
        this.saml2IdpRequestContext = requestContext;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    public Saml2IdpRequestContext getSaml2IdpRequestContext() {
        return saml2IdpRequestContext;
    }
}
