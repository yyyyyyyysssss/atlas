package com.atlas.auth.service;

import com.atlas.auth.config.properties.AtlasOauth2Properties;
import com.atlas.common.core.api.user.dto.ExternalIdentityDTO;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.properties.SecurityProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        return atlasOauth2Properties.getClientName();
    }

    @Override
    public String getAuthorizeUrl() {
        String authorizeCodeUrl = securityProperties.getIssuerUrl() + atlasOauth2Properties.getAuthorizeCodeEndpoint();
        String clientId = atlasOauth2Properties.getClientId();
        String redirectUri = atlasOauth2Properties.getRedirectUrl();
        String scope = atlasOauth2Properties.getScope();
        return UriComponentsBuilder.fromUriString(authorizeCodeUrl)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code") // OAuth2 标准
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .build()
                .encode()
                .toUriString();
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
        log.info("Processing Atlas OAuth2 callback. Client: {}, Code: {}",
                atlasOauth2Properties.getClientName(), code);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("client_id", atlasOauth2Properties.getClientId());
        formData.add("client_secret", atlasOauth2Properties.getClientSecret());
        formData.add("redirect_uri", atlasOauth2Properties.getRedirectUrl());
        if (StringUtils.hasText(codeVerifier)) {
            formData.add("code_verifier", codeVerifier);
        }
        try {
            OAuth2TokenResponse oAuth2TokenResponse = localRestClient
                    .post()
                    .uri(atlasOauth2Properties.getTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        String errorBody = StreamUtils.copyToString(clientResponse.getBody(), StandardCharsets.UTF_8);
                        JsonNode jsonNode = JsonUtils.parseObject(errorBody, JsonNode.class);
                        log.error("OAuth2 Server Error Body: {}", errorBody);
                        throw new BusinessException(jsonNode.get("error").asText());
                    })
                    .body(OAuth2TokenResponse.class);

            String idToken = oAuth2TokenResponse.idToken;
            Jwt jwt = jwtDecoder.decode(idToken);
            ExternalIdentityDTO externalIdentityDTO = ExternalIdentityDTO
                    .builder()
                    .sub(jwt.getSubject())
                    .provider(atlasOauth2Properties.getClientName())
                    .fullName(jwt.getClaimAsString("name"))
                    .avatar(jwt.getClaimAsString("picture"))
                    .email(jwt.getClaimAsString("email"))
                    .phone(jwt.getClaimAsString("phone_number"))
                    .build();
            return doLogin(externalIdentityDTO);
        }catch (Exception e){
            throw new BusinessException(e.getMessage());
        }
    }

    private record OAuth2TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("id_token") String idToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") Integer expiresIn,
            String scope
    ) {}
}
