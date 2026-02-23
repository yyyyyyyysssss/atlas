package com.atlas.auth.config.security.authentication.provider;

import com.atlas.security.token.ThirdPartyAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * @Description 用于提供三方登录的身份认证
 * @Author ys
 * @Date 2024/8/6 9:42
 */
public class ThirdPartyAuthenticationProvider implements AuthenticationProvider {

    private UserDetailsService userDetailsService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            return null;
        }
        ThirdPartyAuthenticationToken thirdPartyAuthenticationToken = (ThirdPartyAuthenticationToken)authentication;
        Object principal = thirdPartyAuthenticationToken.getPrincipal();
        UserDetails userDetails = userDetailsService.loadUserByUsername((String) principal);
        if (userDetails == null) {
            throw new InternalAuthenticationServiceException("UserDetailsService returned null, which is an interface contract violation");
        }
        ThirdPartyAuthenticationToken authenticated = ThirdPartyAuthenticationToken.authenticated(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authenticated.setDetails(authentication.getDetails());
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ThirdPartyAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
}
