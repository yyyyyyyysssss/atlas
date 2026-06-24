package com.atlas.auth.service;

import com.atlas.auth.config.security.oauth2.OAuth2ProviderAuthenticationToken;
import com.atlas.auth.domain.dto.*;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.enums.ThirdPartyAuthAction;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.security.model.TokenResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/8 14:58
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AtlasLoginProvider extends AbstractThirdPartyLoginProvider{

    @Resource
    protected OAuth2ProviderEngine oAuth2ProviderEngine;

    @Override
    public String getProviderName() {
        return "atlas";
    }

    @Override
    public SsoProviderAuthorizeUrlResponse getAuthorizeUrl(ThirdPartyAuthAction action, Map<String, String> extraParams) {
        OAuth2ProviderSettings auth2ProviderSettings = ssoProviderService.getSettings(getProviderName(), SsoProviderProtocol.OAUTH2);
        String state = generateState(action);
        extraParams = extraParams == null ? new HashMap<>() : new HashMap<>(extraParams);
        extraParams.put("state", state);
        return oAuth2ProviderEngine.buildAuthorizeUrl(auth2ProviderSettings, extraParams);
    }

    @Override
    public SsoProviderAuthorizeUrlResponse getQrScanUrl() {
        OAuth2ProviderSettings auth2ProviderSettings = ssoProviderService.getSettings(getProviderName(), SsoProviderProtocol.OAUTH2);
        return oAuth2ProviderEngine.buildQrScanUrl(auth2ProviderSettings);
    }

    @Override
    public TokenResponse authenticate(Authentication authentication) {
        OAuth2ProviderAuthenticationToken authenticationToken = (OAuth2ProviderAuthenticationToken) authentication;
        return processCallback(authenticationToken.code(),authenticationToken.state(),authenticationToken.codeVerifier());
    }

    public TokenResponse processCallback(String code,String state,String codeVerifier) {
        String providerName = getProviderName();
        log.info("Processing Atlas OAuth2 callback. provider: {}, state: {}, code: {}, codeVerifier: {}",
                providerName, state, code, codeVerifier);

        // 校验state
        ThirdPartyStateContext stateContext = validateState(state);

        // 获取配置
        OAuth2ProviderSettings auth2ProviderSettings = ssoProviderService.getSettings(providerName, SsoProviderProtocol.OAUTH2);

        // 获取token
        AtlasTokenResponse atlasTokenResponse = oAuth2ProviderEngine.fetchToken(providerName, auth2ProviderSettings, code, codeVerifier, AtlasTokenResponse.class);
        log.info("AtlasTokenResponse: {}",atlasTokenResponse);

        // 根据token获取用户信息
        OAuth2ProviderToken oAuth2ProviderToken = new OAuth2ProviderToken(atlasTokenResponse.accessToken, atlasTokenResponse.tokenType);
        AtlasUserInfoResponse atlasUserInfoResponse = oAuth2ProviderEngine.fetchUserInfo(providerName, auth2ProviderSettings, oAuth2ProviderToken, AtlasUserInfoResponse.class);
        log.info("AtlasUserInfoResponse : {}", atlasUserInfoResponse);

        Map<String, Object> extraInfo = JsonUtils.convert(atlasUserInfoResponse, new TypeReference<>() {});
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfo
                .builder()
                .provider(providerName)
                .sub(atlasUserInfoResponse.sub)
                .fullName(atlasUserInfoResponse.name)
                .avatar(atlasUserInfoResponse.picture)
                .email(atlasUserInfoResponse.email)
                .emailVerified(atlasUserInfoResponse.emailVerified)
                .phone(atlasUserInfoResponse.phoneNumber)
                .extraInfo(extraInfo)
                .build();
        return dispatchFederatedIdentity(oAuth2UserInfo, stateContext);
    }

    private record AtlasTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("id_token") String idToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") Integer expiresIn,
            String scope
    ) {}

    private record AtlasUserInfoResponse(
            String sub,

            String name,

            @JsonProperty("preferred_username")
            String preferredUsername,

            @JsonProperty("phone_number")
            String phoneNumber,

            String picture,

            String email,

            @JsonProperty("email_verified")
            Boolean emailVerified
    ) {}
}
