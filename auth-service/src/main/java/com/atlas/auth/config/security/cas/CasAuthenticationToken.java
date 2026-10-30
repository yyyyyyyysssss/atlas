package com.atlas.auth.config.security.cas;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/10 17:51
 */
@Getter
public class CasAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;

    private final String ticket;

    private final CasRequestContext casRequestContext;


    public CasAuthenticationToken(CasRequestContext casRequestContext){
        super(null);
        this.principal = casRequestContext;
        this.casRequestContext = casRequestContext;
        this.ticket = null;
        setAuthenticated(false);
    }

    public CasAuthenticationToken(Object principal, String ticket, CasRequestContext casRequestContext, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.ticket = ticket;
        this.casRequestContext = casRequestContext;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }
}
