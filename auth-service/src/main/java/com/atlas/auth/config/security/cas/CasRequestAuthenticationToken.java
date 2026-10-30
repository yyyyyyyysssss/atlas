package com.atlas.auth.config.security.cas;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/12 11:02
 */
@Getter
public class CasRequestAuthenticationToken extends AbstractAuthenticationToken {

    private final String service;

    private final boolean renew;

    private final boolean gateway;

    private final String relayState;

    // 未认证 Token Converter 创建
    public CasRequestAuthenticationToken(String service, boolean renew, boolean gateway, String relayState) {
        super(null);
        this.service = service;
        this.renew = renew;
        this.gateway = gateway;
        this.relayState = relayState;
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }
}
