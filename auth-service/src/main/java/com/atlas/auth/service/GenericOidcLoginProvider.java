package com.atlas.auth.service;

import com.atlas.auth.config.security.oauth2.OAuth2ProviderAuthenticationToken;
import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.enums.ThirdPartyAuthAction;
import com.atlas.security.model.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;

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
    public ThirdPartyCallbackVO authenticate(Authentication authentication) {
        OAuth2ProviderAuthenticationToken authenticationToken = (OAuth2ProviderAuthenticationToken) authentication;

        // 校验并销毁 state 状态
        ThirdPartyStateContext stateContext = validateState(authenticationToken.state());

        // 动态获取当前实例绑定的厂商 OIDC 配置
        OidcProviderSettings settings = ssoProviderService.getSettings(getProviderName(), SsoProviderProtocol.OIDC);

        // 获取用户信息
        OidcUserInfoResult oidcUserInfo = oidcProviderEngine.fetchUserInfo(getProviderName(), settings, authenticationToken.code(), authenticationToken.codeVerifier());

        return dispatchFederatedIdentity(oidcUserInfo, stateContext);
    }
}
