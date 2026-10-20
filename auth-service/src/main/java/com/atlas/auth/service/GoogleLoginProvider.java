package com.atlas.auth.service;

import com.atlas.auth.config.exception.AuthErrorCode;
import com.atlas.auth.config.exception.ThirdPartyAuthException;
import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class GoogleLoginProvider extends AbstractThirdPartyLoginProvider {


    @Resource
    protected OAuth2ProviderEngine oAuth2ProviderEngine;

    @Override
    public String getProviderName() {

        return "google";
    }

    @Override
    public SsoProviderAuthorizeUrlResponse getAuthorizeUrl(ThirdPartyAuthRequestContext requestContext, Map<String, String> extraParams) {
        OAuth2ProviderSettings auth2ProviderSettings = ssoProviderService.getSettings(getProviderName(), SsoProviderProtocol.OAUTH2);
        if (auth2ProviderSettings == null) {
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_PROVIDER_NOT_FOUND, getProviderName(), SsoProviderProtocol.OAUTH2);
        }
        String state = generateState(requestContext);
        extraParams = extraParams == null ? new HashMap<>() : new HashMap<>(extraParams);
        extraParams.put("state", state);
        return oAuth2ProviderEngine.buildAuthorizeUrl(auth2ProviderSettings, extraParams);
    }

    @Override
    public ThirdPartyCallbackVO authenticate(ThirdPartyLoginRequest request) {
        OAuth2LoginRequest oauth2 = (OAuth2LoginRequest) request;
        return processCallback(oauth2.code(), oauth2.state(), oauth2.codeVerifier());
    }

    public ThirdPartyCallbackVO processCallback(String code, String state, String codeVerifier) {
        String providerName = getProviderName();
        log.info("Processing Google OAuth2 callback. provider: {}, state: {}, code: {}, codeVerifier: {}",
                providerName, state, code, codeVerifier);

        // 校验state
        ThirdPartyStateContext stateContext = validateState(state);

        // 获取配置
        OAuth2ProviderSettings auth2ProviderSettings = ssoProviderService.getSettings(providerName, SsoProviderProtocol.OAUTH2);
        if (auth2ProviderSettings == null) {
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_PROVIDER_NOT_FOUND, getProviderName(), SsoProviderProtocol.OAUTH2);
        }
        OAuth2UserInfo oAuth2UserInfo;
        try {
            // 获取token
            GoogleTokenResponse googleTokenResponse = oAuth2ProviderEngine.fetchToken(providerName, auth2ProviderSettings, code, codeVerifier, GoogleTokenResponse.class);
            log.info("GoogleTokenResponse: {}", googleTokenResponse);

            // 根据token获取用户信息
            OAuth2ProviderToken oAuth2ProviderToken = new OAuth2ProviderToken(googleTokenResponse.accessToken, googleTokenResponse.tokenType);
            GoogleUserInfoResponse googleUserInfoResponse = oAuth2ProviderEngine.fetchUserInfo(providerName, auth2ProviderSettings, oAuth2ProviderToken, GoogleUserInfoResponse.class);

            if (googleUserInfoResponse.emailVerified() != null && !googleUserInfoResponse.emailVerified()) {
                throw new BusinessException("Google 账号邮箱未验证，安全起见拒绝登录。请先前往 Google 账户完成邮箱验证。");
            }
            Map<String, Object> extraInfo = JsonUtils.convert(googleUserInfoResponse, new TypeReference<>() {
            });
            oAuth2UserInfo = OAuth2UserInfo
                    .builder()
                    .sub(googleUserInfoResponse.sub)
                    .provider(providerName)
                    .fullName(googleUserInfoResponse.familyName + googleUserInfoResponse.givenName)
                    .avatar(googleUserInfoResponse.picture)
                    .email(googleUserInfoResponse.email)
                    .emailVerified(googleUserInfoResponse.emailVerified)
                    .extraInfo(extraInfo)
                    .build();
        } catch (OAuth2ProviderEngine.OAuth2ProviderException e) {
            log.warn("[{}] OAUTH2 认证失败", getProviderName(), e);
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_AUTH_FAILED, getProviderName(), SsoProviderProtocol.OAUTH2);
        }

        return dispatchFederatedIdentity(oAuth2UserInfo, stateContext);
    }

    /**
     * @Description Google Token 响应对象
     * @Author ys
     * @Date 2024/8/3 17:59
     */
    public record GoogleTokenResponse(

            @JsonProperty("access_token")
            String accessToken,

            @JsonProperty("expires_in")
            Long expiresIn,

            @JsonProperty("token_type")
            String tokenType,

            String scope,

            @JsonProperty("refresh_token")
            String refreshToken,

            @JsonProperty("id_token")
            String idToken
    ) {

    }

    /**
     * @Description Google 用户信息响应对象
     * @Author ys
     * @Date 2024/8/3 23:44
     */
    public record GoogleUserInfoResponse(
            String sub,

            String name,

            @JsonProperty("given_name")
            String givenName,

            @JsonProperty("family_name")
            String familyName,

            String picture,

            String email,

            @JsonProperty("email_verified")
            Boolean emailVerified
    ) {

    }

}
