package com.atlas.auth.config.security.oauth2.provider;

import com.atlas.auth.service.QrAuthService;
import com.atlas.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/24 15:45
 */
@RequiredArgsConstructor
public class OAuth2QrAuthorizationCodeRequestAuthenticationProvider implements AuthenticationProvider {

    private final QrAuthService qrAuthService;

    private final UserService userService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication = (OAuth2AuthorizationCodeRequestAuthenticationToken) authentication;
        Map<String, Object> additionalParameters = authorizationCodeRequestAuthentication.getAdditionalParameters();
        String sceneId = (String) additionalParameters.get("scene_id");
        if (!StringUtils.hasText(sceneId)) {
            return null;
        }
        Long userId = qrAuthService.verifyAndConsumeScene(sceneId);

        UserDetails userDetails = userService.loadUserByUserId(userId);

        UsernamePasswordAuthenticationToken principal = UsernamePasswordAuthenticationToken.authenticated(userDetails, null, userDetails.getAuthorities());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(principal);

        return authorizationCodeRequestAuthentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2AuthorizationCodeRequestAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
