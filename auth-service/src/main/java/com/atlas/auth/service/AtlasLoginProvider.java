package com.atlas.auth.service;

import com.atlas.auth.config.properties.AtlasOauth2Properties;
import com.atlas.common.core.api.user.UserApi;
import com.atlas.common.core.api.user.dto.ExternalIdentityDTO;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.response.Result;
import com.atlas.security.enums.ClientType;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.token.ThirdPartyAuthenticationToken;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/8 14:58
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AtlasLoginProvider implements ThirdPartyLoginProvider{

    private final AtlasOauth2Properties atlasOauth2Properties;

    private final RestClient localRestClient;

    private final JwtDecoder jwtDecoder;

    private final UserApi userApi;

    private final LoginService loginService;

    @Override
    public String getProviderName() {
        return atlasOauth2Properties.getClientName();
    }

    @Override
    public String getAuthorizeUrl() {
        return atlasOauth2Properties.getAuthorizeCodeUrl();
    }

    @Override
    public String getQrScanUrl() {
        return atlasOauth2Properties.getQrScanUrl();
    }

    @Override
    public TokenResponse processCallback(String code,String state) {
        log.info("Processing Atlas OAuth2 callback. Client: {}, Code: {}",
                atlasOauth2Properties.getClientName(), code);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("client_id", atlasOauth2Properties.getClientId());
        formData.add("client_secret", atlasOauth2Properties.getClientSecret());
        formData.add("redirect_uri", atlasOauth2Properties.getRedirectUrl());
        try {
            OAuth2TokenResponse oAuth2TokenResponse = localRestClient
                    .post()
                    .uri(atlasOauth2Properties.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        log.error("Token exchange failed. Status: {}, ClientId: {}",
                                clientResponse.getStatusCode(), atlasOauth2Properties.getClientId());
                        throw new BusinessException("认证服务器获取token失败: " + clientResponse.getStatusCode());
                    })
                    .body(OAuth2TokenResponse.class);

            String idToken = oAuth2TokenResponse.idToken;
            Jwt jwt = jwtDecoder.decode(idToken);
            String sub = jwt.getSubject();
            String name = jwt.getClaimAsString("name");
            String avatar = jwt.getClaimAsString("picture");
            String email = jwt.getClaimAsString("email");
            String phone = jwt.getClaimAsString("phone_number");
            ExternalIdentityDTO externalIdentityDTO = ExternalIdentityDTO
                    .builder()
                    .sub(sub)
                    .provider(atlasOauth2Properties.getClientName())
                    .fullName(name)
                    .avatar(avatar)
                    .email(email)
                    .phone(phone)
                    .build();
            Result<UserDTO> userResult = userApi.ensureUser(externalIdentityDTO);
            if(!userResult.isSucceed()){
                throw new BusinessException("获取或注册用户失败: " + userResult.getMessage());
            }
            UserDTO userDTO = userResult.getData();
            ThirdPartyAuthenticationToken thirdPartyAuthenticationToken = new ThirdPartyAuthenticationToken(userDTO.getUsername(), null);
            return loginService.login(thirdPartyAuthenticationToken, ClientType.WEB, true, false);
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
