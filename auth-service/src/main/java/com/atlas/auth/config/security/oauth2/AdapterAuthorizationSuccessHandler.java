package com.atlas.auth.config.security.oauth2;

import com.atlas.common.core.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/11 13:19
 */
public class AdapterAuthorizationSuccessHandler implements AuthenticationSuccessHandler {

    private final RedirectStrategy redirectStrategy;

    public AdapterAuthorizationSuccessHandler(){
        this.redirectStrategy = new DefaultRedirectStrategy();

    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if(isJsonRequest(request)){
            sendAuthorizationJsonResponse(request,response,authentication);
        } else {
            sendAuthorizationRedirectResponse(request,response,authentication);
        }
    }

    private void sendAuthorizationJsonResponse(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication = (OAuth2AuthorizationCodeRequestAuthenticationToken)authentication;
        String code = authorizationCodeRequestAuthentication.getAuthorizationCode().getTokenValue();
        String state = authorizationCodeRequestAuthentication.getState();
        AuthorizationResponse authResponse = new AuthorizationResponse(code, state, "success");

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JsonUtils.toJson(authResponse));
    }

    private void sendAuthorizationRedirectResponse(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication = (OAuth2AuthorizationCodeRequestAuthenticationToken)authentication;
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(authorizationCodeRequestAuthentication.getRedirectUri()).queryParam("code", new Object[]{authorizationCodeRequestAuthentication.getAuthorizationCode().getTokenValue()});
        if (StringUtils.hasText(authorizationCodeRequestAuthentication.getState())) {
            uriBuilder.queryParam("state", new Object[]{UriUtils.encode(authorizationCodeRequestAuthentication.getState(), StandardCharsets.UTF_8)});
        }

        String redirectUri = uriBuilder.build(true).toUriString();
        this.redirectStrategy.sendRedirect(request, response, redirectUri);
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) ||
                "json".equals(request.getParameter("format"));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AuthorizationResponse(
            String code,
            String state,
            String message
    ) {}
}
