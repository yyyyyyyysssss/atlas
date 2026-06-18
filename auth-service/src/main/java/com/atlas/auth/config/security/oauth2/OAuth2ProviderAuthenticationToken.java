package com.atlas.auth.config.security.oauth2;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * 纯数据载体，禁止调用除获取参数外的任何 Authentication 方法
 */
public record OAuth2ProviderAuthenticationToken(String code, String state,
                                                String codeVerifier) implements Authentication {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        throw new UnsupportedOperationException("该对象仅作为数据载体，不可获取权限信息");
    }

    @Override
    public Object getCredentials() {
        throw new UnsupportedOperationException("该对象仅作为数据载体，不可获取凭证");
    }

    @Override
    public Object getDetails() {
        throw new UnsupportedOperationException("该对象仅作为数据载体，不可获取详情");
    }

    @Override
    public Object getPrincipal() {
        throw new UnsupportedOperationException("该对象仅作为数据载体，不可获取主体");
    }

    @Override
    public boolean isAuthenticated() {
        throw new UnsupportedOperationException("该对象仅作为数据载体，不可校验状态");
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        throw new UnsupportedOperationException("该对象仅作为数据载体，不可设置认证状态");
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("该对象仅作为数据载体，不可获取名称");
    }
}