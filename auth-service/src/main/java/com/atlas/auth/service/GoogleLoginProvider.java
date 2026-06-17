package com.atlas.auth.service;

import com.atlas.auth.domain.dto.OAuth2ProviderSettings;
import com.atlas.auth.domain.dto.OAuth2ProviderToken;
import com.atlas.auth.domain.dto.OAuth2UserInfo;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.security.model.TokenResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/8 14:58
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleLoginProvider extends AbstractThirdPartyLoginProvider{


    @Override
    public String getProviderName() {

        return "google";
    }

    @Override
    public String getAuthorizeUrl() {
        OAuth2ProviderSettings auth2ProviderSettings = ssoProviderService.getSettings(getProviderName(), SsoProviderProtocol.OAUTH2, OAuth2ProviderSettings.class);
        String state = generateState();
        Map<String, String> extraParams = Map.of(
                "state", state
        );
        return oAuth2ProviderEngine.buildAuthorizeUrl(auth2ProviderSettings, extraParams);
    }

    @Override
    public boolean isPKCERequired() {
        return true;
    }

    @Override
    public TokenResponse processCallback(String code,String state,String codeVerifier) {
        String providerName = getProviderName();
        log.info("Processing Google OAuth2 callback. provider: {}, state: {}, code: {}, codeVerifier: {}",
                providerName, state, code, codeVerifier);

        // 校验state
        validateState(state);

        // 获取配置
        OAuth2ProviderSettings auth2ProviderSettings = ssoProviderService.getSettings(providerName, SsoProviderProtocol.OAUTH2, OAuth2ProviderSettings.class);

        // 获取token
        GoogleTokenResponse googleTokenResponse = oAuth2ProviderEngine.fetchToken(providerName, auth2ProviderSettings, code, codeVerifier, GoogleTokenResponse.class);
        log.info("GoogleTokenResponse: {}",googleTokenResponse);

        // 根据token获取用户信息
        OAuth2ProviderToken oAuth2ProviderToken = new OAuth2ProviderToken(googleTokenResponse.accessToken, googleTokenResponse.tokenType);
        GoogleUserInfoResponse googleUserInfoResponse = oAuth2ProviderEngine.fetchUserInfo(providerName, auth2ProviderSettings, oAuth2ProviderToken, GoogleUserInfoResponse.class);

        if (googleUserInfoResponse.emailVerified() != null && !googleUserInfoResponse.emailVerified()) {
            throw new BusinessException("Google 账号邮箱未验证，安全起见拒绝登录。请先前往 Google 账户完成邮箱验证。");
        }
        Map<String, Object> extraInfo = JsonUtils.convert(googleUserInfoResponse, new TypeReference<>() {});
        OAuth2UserInfo externalIdentityDTO = OAuth2UserInfo
                .builder()
                .sub(googleUserInfoResponse.sub)
                .provider(providerName)
                .fullName(googleUserInfoResponse.familyName + googleUserInfoResponse.givenName)
                .avatar(googleUserInfoResponse.picture)
                .email(googleUserInfoResponse.email)
                .emailVerified(googleUserInfoResponse.emailVerified)
                .extraInfo(extraInfo)
                .build();
        return doLogin(externalIdentityDTO);
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
