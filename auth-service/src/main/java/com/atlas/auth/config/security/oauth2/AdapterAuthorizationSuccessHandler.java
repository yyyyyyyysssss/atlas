package com.atlas.auth.config.security.oauth2;

import com.atlas.auth.enums.ClientProtocol;
import com.atlas.auth.event.ClientAuthorizedEvent;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.common.security.model.SecurityUser;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.Objects;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/11 13:19
 */
public class AdapterAuthorizationSuccessHandler implements AuthenticationSuccessHandler {

    private RedirectStrategy redirectStrategy;

    private final ApplicationEventPublisher eventPublisher;

    public AdapterAuthorizationSuccessHandler(ApplicationEventPublisher eventPublisher) {
        this.redirectStrategy = new DefaultRedirectStrategy();
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication = (OAuth2AuthorizationCodeRequestAuthenticationToken) authentication;
        // 发布授权成功事件
        if (eventPublisher != null) {
            String clientId = authorizationCodeRequestAuthentication.getClientId();
            SecurityUser securityUser = getSecurityUser(authorizationCodeRequestAuthentication.getPrincipal());
            eventPublisher.publishEvent(new ClientAuthorizedEvent(this, securityUser.getId(), clientId, ClientProtocol.OAUTH2));
        }
        if (isJsonRequest(request)) {
            sendAuthorizationJsonResponse(request, response, authorizationCodeRequestAuthentication);
        } else {
            sendAuthorizationRedirectResponse(request, response, authorizationCodeRequestAuthentication);
        }
    }

    public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
        Objects.requireNonNull(redirectStrategy);
        this.redirectStrategy = redirectStrategy;
    }

    private void sendAuthorizationJsonResponse(HttpServletRequest request, HttpServletResponse response, OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication) throws IOException {
        String code = authorizationCodeRequestAuthentication.getAuthorizationCode().getTokenValue();
        String state = authorizationCodeRequestAuthentication.getState();
        AuthorizationResponse authResponse = new AuthorizationResponse(code, state, "success");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JsonUtils.toJson(authResponse));
    }

    private void sendAuthorizationRedirectResponse(HttpServletRequest request, HttpServletResponse response, OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication) throws IOException {
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

    private SecurityUser getSecurityUser(Object principal) {
        while (principal instanceof Authentication authentication) {
            principal = authentication.getPrincipal();
        }
        return principal instanceof SecurityUser ? (SecurityUser) principal : null;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AuthorizationResponse(
            String code,
            String state,
            String message
    ) {
    }
}
