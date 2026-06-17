package com.atlas.auth.service;

import com.atlas.auth.config.properties.AtlasOauth2Properties;
import com.atlas.auth.domain.dto.OAuth2ProviderSettings;
import com.atlas.auth.domain.dto.OAuth2ProviderToken;
import com.atlas.auth.domain.dto.OAuth2UserInfo;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.properties.SecurityProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
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

    private final SecurityProperties securityProperties;

    private final AtlasOauth2Properties atlasOauth2Properties;

    private final RestClient localRestClient;

    private final JwtDecoder jwtDecoder;

    @Override
    public String getProviderName() {
        return "atlas";
    }

    @Override
    public String getAuthorizeUrl() {
        OAuth2ProviderSettings auth2ProviderSettings = ssoProviderService.getSettings(getProviderName(), SsoProviderProtocol.OAUTH2, OAuth2ProviderSettings.class);
        String state = generateState();
        Map<String, String> extraParams = Map.of(
                "state", state
        );
        return oAuth2ProviderEngine.buildAuthorizeUrl(auth2ProviderSettings, extraParams);

//        String authorizeCodeUrl = securityProperties.getIssuerUrl() + atlasOauth2Properties.getAuthorizeCodeEndpoint();
//        String clientId = atlasOauth2Properties.getClientId();
//        String redirectUri = atlasOauth2Properties.getRedirectUrl();
//        String scope = atlasOauth2Properties.getScope();
//        return UriComponentsBuilder.fromUriString(authorizeCodeUrl)
//                .queryParam("client_id", clientId)
//                .queryParam("response_type", "code") // OAuth2 标准
//                .queryParam("redirect_uri", redirectUri)
//                .queryParam("scope", scope)
//                .build()
//                .encode()
//                .toUriString();
    }

    @Override
    public boolean isPKCERequired() {
        return true;
    }

    @Override
    public String getQrScanUrl() {
        String clientId = atlasOauth2Properties.getClientId();
        String scope = atlasOauth2Properties.getScope();
        String redirectUri = atlasOauth2Properties.getRedirectUrl();
        return UriComponentsBuilder.fromUriString(atlasOauth2Properties.getQrScanEndpoint())
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code") // OAuth2 标准
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public TokenResponse processCallback(String code,String state,String codeVerifier) {
        String providerName = getProviderName();
        log.info("Processing Atlas OAuth2 callback. provider: {}, state: {}, code: {}, codeVerifier: {}",
                providerName, state, code, codeVerifier);

        // 校验state
        validateState(state);

        // 获取配置
        OAuth2ProviderSettings auth2ProviderSettings = ssoProviderService.getSettings(providerName, SsoProviderProtocol.OAUTH2, OAuth2ProviderSettings.class);

        // 获取token
        AtlasTokenResponse atlasTokenResponse = oAuth2ProviderEngine.fetchToken(providerName, auth2ProviderSettings, code, codeVerifier, AtlasTokenResponse.class);
        log.info("AtlasTokenResponse: {}",atlasTokenResponse);
        String accessToken = atlasTokenResponse.accessToken;

        // 根据token获取用户信息
        OAuth2ProviderToken oAuth2ProviderToken = new OAuth2ProviderToken(atlasTokenResponse.accessToken, atlasTokenResponse.tokenType);
        AtlasUserInfoResponse atlasUserInfoResponse = oAuth2ProviderEngine.fetchUserInfo(providerName, auth2ProviderSettings, oAuth2ProviderToken, AtlasUserInfoResponse.class);
        log.info("AtlasUserInfoResponse : {}", atlasUserInfoResponse);

        Map<String, Object> extraInfo = JsonUtils.convert(atlasUserInfoResponse, new TypeReference<>() {});
        OAuth2UserInfo externalIdentityDTO = OAuth2UserInfo
                .builder()
                .sub(atlasUserInfoResponse.sub())
                .provider(providerName)
                .fullName(atlasUserInfoResponse.name)
                .avatar(atlasUserInfoResponse.picture)
                .email(atlasUserInfoResponse.email)
                .emailVerified(atlasUserInfoResponse.emailVerified)
                .phone(atlasUserInfoResponse.phoneNumber)
                .extraInfo(extraInfo)
                .build();

        return doLogin(externalIdentityDTO);

//        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
//        formData.add("grant_type", "authorization_code");
//        formData.add("code", code);
//        formData.add("client_id", atlasOauth2Properties.getClientId());
//        formData.add("client_secret", atlasOauth2Properties.getClientSecret());
//        formData.add("redirect_uri", atlasOauth2Properties.getRedirectUrl());
//        if (StringUtils.hasText(codeVerifier)) {
//            formData.add("code_verifier", codeVerifier);
//        }
//        try {
//            OAuth2TokenResponse oAuth2TokenResponse = localRestClient
//                    .post()
//                    .uri(atlasOauth2Properties.getTokenEndpoint())
//                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                    .body(formData)
//                    .retrieve()
//                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
//                        String errorBody = StreamUtils.copyToString(clientResponse.getBody(), StandardCharsets.UTF_8);
//                        JsonNode jsonNode = JsonUtils.parseObject(errorBody, JsonNode.class);
//                        log.error("OAuth2 Server Error Body: {}", errorBody);
//                        throw new BusinessException(jsonNode.get("error").asText());
//                    })
//                    .body(OAuth2TokenResponse.class);
//
//            String idToken = oAuth2TokenResponse.idToken;
//            Jwt jwt = jwtDecoder.decode(idToken);
//            Map<String, Object> claims = jwt.getClaims();
//            OAuth2UserInfo externalIdentityDTO = OAuth2UserInfo
//                    .builder()
//                    .sub(jwt.getSubject())
//                    .provider(atlasOauth2Properties.getClientName())
//                    .fullName(jwt.getClaimAsString("name"))
//                    .avatar(jwt.getClaimAsString("picture"))
//                    .email(jwt.getClaimAsString("email"))
//                    .phone(jwt.getClaimAsString("phone_number"))
//                    .extraInfo(claims)
//                    .build();
//            return doLogin(externalIdentityDTO);
//        }catch (Exception e){
//            throw new BusinessException(e.getMessage());
//        }
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
