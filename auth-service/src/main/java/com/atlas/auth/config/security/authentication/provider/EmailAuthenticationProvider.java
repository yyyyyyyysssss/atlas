package com.atlas.auth.config.security.authentication.provider;

import com.atlas.auth.service.EmailVerificationService;
import com.atlas.security.token.EmailAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * @Description
 * @Author ys
 * @Date 2024/8/13 11:38
 */
public class EmailAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;

    private final EmailVerificationService emailVerificationService;

    public EmailAuthenticationProvider(UserDetailsService userDetailsService, EmailVerificationService emailVerificationService){
        this.userDetailsService = userDetailsService;
        this.emailVerificationService = emailVerificationService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            return null;
        }
        EmailAuthenticationToken emailAuthenticationToken = (EmailAuthenticationToken) authentication;
        Object principal = emailAuthenticationToken.getPrincipal();
        Object credentials = emailAuthenticationToken.getCredentials();
        UserDetails userDetails = userDetailsService.loadUserByUsername((String) principal);
        if (userDetails == null) {
            throw new InternalAuthenticationServiceException("UserDetailsService returned null, which is an interface contract violation");
        }
        boolean verify = emailVerificationService.verify((String) principal, (String)credentials);
        if (!verify){
            throw new BadCredentialsException("验证码错误!");
        }
        EmailAuthenticationToken authenticated = EmailAuthenticationToken.authenticated(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authenticated.setDetails(emailAuthenticationToken.getDetails());
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return EmailAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
