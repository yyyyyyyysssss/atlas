package com.atlas.auth.config.security.authentication.provider;

import com.atlas.auth.domain.dto.WebauthnAuthenticateResponse;
import com.atlas.auth.service.UserService;
import com.atlas.auth.service.WebauthnService;
import com.atlas.security.token.WebauthnAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/26 14:57
 */
public class WebauthnAuthenticationProvider implements AuthenticationProvider {

    private final WebauthnService webauthnService;
    private final UserService userService;

    public WebauthnAuthenticationProvider(WebauthnService webauthnService, UserService userService) {
        Assert.notNull(webauthnService, "webauthnService cannot be null");
        Assert.notNull(userService, "userService cannot be null");
        this.webauthnService = webauthnService;
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        WebauthnAuthenticationToken webauthnAuthenticationToken = (WebauthnAuthenticationToken) authentication;
        try {
            WebauthnAuthenticateResponse authenticate = this.webauthnService.authenticate(webauthnAuthenticationToken.getWebAuthnRequest());
            Long userId = authenticate.userId();
            UserDetails userDetails = userService.loadUserByUserId(userId);
            return new WebauthnAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        } catch (RuntimeException e) {
            throw new BadCredentialsException(e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return WebauthnAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
