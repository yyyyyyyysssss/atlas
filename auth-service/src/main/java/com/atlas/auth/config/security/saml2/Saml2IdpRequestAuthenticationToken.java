package com.atlas.auth.config.security.saml2;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/12 11:02
 */
public class Saml2IdpRequestAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;

    private Object credentials;

    private final String samlRequest;

    private final String binding;

    private final String relayState;

    // 原始 QueryString Redirect Binding验签需要
    private final String rawQueryString;

    // 未认证 Token Converter 创建
    public Saml2IdpRequestAuthenticationToken(String samlRequest, String binding, String relayState, String rawQueryString) {
        super(null);
        this.principal = null;
        this.credentials = samlRequest;
        this.samlRequest = samlRequest;
        this.binding = binding;
        this.relayState = relayState;
        this.rawQueryString = rawQueryString;
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    public String getSamlRequest() {
        return samlRequest;
    }

    public String getBinding() {
        return binding;
    }

    public String getRelayState() {
        return relayState;
    }

    public String getRawQueryString() {
        return rawQueryString;
    }

}
