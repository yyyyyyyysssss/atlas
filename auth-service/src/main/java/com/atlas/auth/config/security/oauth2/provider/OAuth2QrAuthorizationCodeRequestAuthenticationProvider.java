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
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
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
        if (!supports(authentication.getClass())) {
            return null;
        }
        OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication = (OAuth2AuthorizationCodeRequestAuthenticationToken) authentication;
        Map<String, Object> additionalParameters = authorizationCodeRequestAuthentication.getAdditionalParameters();
        String sceneId = (String) additionalParameters.get("scene_id");
        if (!StringUtils.hasText(sceneId)) {
            return null;
        }
        // 验证并消费扫码场景 ID，获取对应的用户 ID
        Long userId = qrAuthService.verifyAndConsumeScene(sceneId);
        if (userId == null) {
            // 如果 scene_id 无效或已过期，可以抛出异常，触发错误处理器
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_scene_id", "二维码已失效或不存在", null));
        }
        // 加载用户信息
        UserDetails userDetails = userService.loadUserByUserId(userId);
        // 构建已认证的 Principal
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
