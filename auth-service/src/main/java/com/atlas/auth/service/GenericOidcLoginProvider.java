package com.atlas.auth.service;

import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/23 14:16
 */
@Slf4j
@RequiredArgsConstructor
public class GenericOidcLoginProvider extends AbstractThirdPartyLoginProvider{

    private final String providerName;

    private final OidcProviderEngine oidcProviderEngine;

    @Override
    public String getProviderName() {
        return this.providerName;
    }

    @Override
    public SsoProviderProtocol protocol() {
        return SsoProviderProtocol.OIDC;
    }

    @Override
    public SsoProviderAuthorizeUrlResponse getAuthorizeUrl(ThirdPartyAuthRequestContext requestContext, Map<String, String> extraParams) {
        OidcProviderSettings settings = ssoProviderService.getSettings(getProviderName(), SsoProviderProtocol.OIDC);
        String state = generateState(requestContext);
        extraParams = extraParams == null ? new HashMap<>() : new HashMap<>(extraParams);
        extraParams.put("state", state);
        return oidcProviderEngine.buildAuthorizeUrl(getProviderName(), settings, extraParams);
    }

    @Override
    public ThirdPartyCallbackVO authenticate(ThirdPartyLoginRequest request) {
        OAuth2LoginRequest oauth2 = (OAuth2LoginRequest)request;

        // 校验并销毁 state 状态
        ThirdPartyStateContext stateContext = validateState(oauth2.state());

        // 动态获取当前实例绑定的厂商 OIDC 配置
        OidcProviderSettings settings = ssoProviderService.getSettings(getProviderName(), SsoProviderProtocol.OIDC);

        // 获取用户信息
        OidcUserInfoResult oidcUserInfo = oidcProviderEngine.fetchUserInfo(getProviderName(), settings, oauth2.code(), oauth2.codeVerifier());

        return dispatchFederatedIdentity(oidcUserInfo, stateContext);
    }
}
