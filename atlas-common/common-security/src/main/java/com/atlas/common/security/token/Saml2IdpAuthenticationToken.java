package com.atlas.common.security.token;

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
    private String oldTokenId;

    public Saml2IdpAuthenticationToken(Object principal, Object credentials) {
        super(null);
        this.principal = principal;
        this.credentials = credentials;
        this.setAuthenticated(false);
    }

    public Saml2IdpAuthenticationToken(Object principal, Object credentials,String oldTokenId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        this.oldTokenId = oldTokenId;
        super.setAuthenticated(true);
    }


    public static RefreshAuthenticationToken unauthenticated(Object principal, Object credentials) {
        return new RefreshAuthenticationToken(principal, credentials);
    }

    public static RefreshAuthenticationToken authenticated(Object principal, Object credentials,String oldTokenId, Collection<? extends GrantedAuthority> authorities) {
        return new RefreshAuthenticationToken(principal, credentials,oldTokenId, authorities);
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }
}
