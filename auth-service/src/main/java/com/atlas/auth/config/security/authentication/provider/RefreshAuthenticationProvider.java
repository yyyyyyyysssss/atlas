package com.atlas.auth.config.security.authentication.provider;

import com.atlas.auth.service.UserService;
import com.atlas.security.enums.TokenType;
import com.atlas.security.model.PayloadInfo;
import com.atlas.security.service.TokenService;
import com.atlas.security.token.RefreshAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @Description 用于提供三方登录的身份认证
 * @Author ys
 * @Date 2024/8/6 9:42
 */
public class RefreshAuthenticationProvider implements AuthenticationProvider {

    private final UserService userService;

    private final TokenService tokenService;

    public RefreshAuthenticationProvider(UserService userService,TokenService tokenService){
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            return null;
        }
        RefreshAuthenticationToken refreshAuthenticationToken = (RefreshAuthenticationToken) authentication;
        String token = (String)refreshAuthenticationToken.getCredentials();
        PayloadInfo payloadInfo = tokenService.verify(token, TokenType.REFRESH_TOKEN);
        String userId = payloadInfo.getSubject();
        UserDetails userDetails = userService.loadUserByUserId(Long.parseLong(userId));
        if (userDetails == null) {
            throw new InternalAuthenticationServiceException("UserDetailsService returned null, which is an interface contract violation");
        }
        RefreshAuthenticationToken authenticated = RefreshAuthenticationToken.authenticated(
                userDetails,
                null,
                payloadInfo.getId(),
                userDetails.getAuthorities()
        );
        authenticated.setDetails(authentication.getDetails());
        tokenService.revoke(token);
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return RefreshAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
