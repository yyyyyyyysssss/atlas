package com.atlas.auth.config.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
public class OAuth2TokenResponseAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final HttpMessageConverter<OAuth2AccessTokenResponse> accessTokenResponseConverter = new OAuth2AccessTokenResponseHttpMessageConverter();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof OAuth2AccessTokenAuthenticationToken accessTokenAuthentication) {
            OAuth2AccessToken accessToken = accessTokenAuthentication.getAccessToken();
            OAuth2RefreshToken refreshToken = accessTokenAuthentication.getRefreshToken();
            Map<String, Object> additionalParameters = accessTokenAuthentication.getAdditionalParameters();

            // --- 核心逻辑：强制覆盖 TokenType ---
            OAuth2AccessTokenResponse.Builder builder = OAuth2AccessTokenResponse.withToken(accessToken.getTokenValue())
                    .tokenType(new OAuth2AccessToken.TokenType("token")) // 强制设置为 "token"
                    .scopes(accessToken.getScopes());

            if (accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null) {
                builder.expiresIn(ChronoUnit.SECONDS.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()));
            }

            if (refreshToken != null) {
                builder.refreshToken(refreshToken.getTokenValue());
            }

            if (!CollectionUtils.isEmpty(additionalParameters)) {
                builder.additionalParameters(additionalParameters);
            }

            OAuth2AccessTokenResponse accessTokenResponse = builder.build();
            ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
            this.accessTokenResponseConverter.write(accessTokenResponse, MediaType.APPLICATION_JSON, httpResponse);
        } else {
            log.error("Authentication must be of type OAuth2AccessTokenAuthenticationToken");
            throw new OAuth2AuthenticationException(new OAuth2Error("server_error", "Unable to process the access token response.", null));
        }
    }
}