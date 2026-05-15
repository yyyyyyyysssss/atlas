package com.atlas.auth.service;

import com.atlas.auth.config.properties.GoogleOauth2Properties;
import com.atlas.common.core.api.user.dto.ExternalIdentityDTO;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.security.model.TokenResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

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

    private final GoogleOauth2Properties googleOauth2Properties;

    private final RestClient proxyRestClient;

    @Override
    public String getProviderName() {
        return googleOauth2Properties.getClientName();
    }

    @Override
    public String getAuthorizeUrl() {
        String authorizeCodeEndpoint = googleOauth2Properties.getAuthorizeCodeEndpoint();
        String clientId = googleOauth2Properties.getClientId();
        String redirectUri = googleOauth2Properties.getRedirectUrl();
        String scope = googleOauth2Properties.getScope();
        return UriComponentsBuilder.fromUriString(authorizeCodeEndpoint)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code") // OAuth2 标准
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", "Google")
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public TokenResponse processCallback(String code,String state,String codeVerifier) {
        log.info("Processing Google OAuth2 callback. Client: {}, Code: {}",
                googleOauth2Properties.getClientName(), code);
        //获取token
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", googleOauth2Properties.getClientId());
        body.add("client_secret", googleOauth2Properties.getClientSecret());
        body.add("code", code);
        body.add("redirect_uri", googleOauth2Properties.getRedirectUrl());
        body.add("grant_type", "authorization_code");
        if (StringUtils.hasText(codeVerifier)) {
            body.add("code_verifier", codeVerifier);
        }
        GoogleTokenResponse googleTokenResponse = proxyRestClient
                .post()
                .uri(googleOauth2Properties.getTokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED) // 必须是表单格式
                .body(body)
                .retrieve()
                .body(GoogleTokenResponse.class);
        log.info("GoogleTokenResponse: {}",googleTokenResponse);

        GoogleUserInfoResponse googleUserInfoResponse = proxyRestClient
                .post()
                .uri(googleOauth2Properties.getUserInfoEndpoint())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + googleTokenResponse.accessToken)
                .retrieve()
                .body(GoogleUserInfoResponse.class);
        if (googleUserInfoResponse.emailVerified() != null && !googleUserInfoResponse.emailVerified()) {
            throw new BusinessException("Google 账号邮箱未验证，安全起见拒绝登录。请先前往 Google 账户完成邮箱验证。");
        }
        Map<String, Object> extraInfo = JsonUtils.convert(googleUserInfoResponse, new TypeReference<>() {});
        ExternalIdentityDTO externalIdentityDTO = ExternalIdentityDTO
                .builder()
                .sub(googleUserInfoResponse.sub)
                .provider(googleOauth2Properties.getClientName())
                .fullName(googleUserInfoResponse.familyName + googleUserInfoResponse.givenName)
                .avatar(googleUserInfoResponse.picture)
                .email(googleUserInfoResponse.email)
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
