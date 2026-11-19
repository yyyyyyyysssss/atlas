package com.atlas.auth.config.security.provider;

import com.atlas.auth.service.UserService;
import com.atlas.common.security.token.ThirdPartyAuthenticationToken;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

/**
 * @Description 用于提供三方登录的身份认证
 * @Author ys
 * @Date 2024/8/6 9:42
 */
public class ThirdPartyAuthenticationProvider implements AuthenticationProvider {

    private UserService userService;

    private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

    public ThirdPartyAuthenticationProvider(UserService userService){
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            return null;
        }
        ThirdPartyAuthenticationToken thirdPartyAuthenticationToken = (ThirdPartyAuthenticationToken)authentication;
        Object principal = thirdPartyAuthenticationToken.getPrincipal();
        UserDetails userDetails = userService.loadUserByUserId((Long) principal);

        // 校验用户状态（是否被禁用、锁定等）
        userDetailsChecker.check(userDetails);

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
}
