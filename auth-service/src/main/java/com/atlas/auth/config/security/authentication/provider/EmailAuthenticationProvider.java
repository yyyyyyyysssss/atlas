package com.atlas.auth.config.security.authentication.provider;

import com.atlas.auth.enums.IdentifierType;
import com.atlas.auth.enums.VerificationScene;
import com.atlas.auth.service.EmailVerificationService;
import com.atlas.auth.service.UserService;
import com.atlas.security.token.EmailAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @Description
 * @Author ys
 * @Date 2024/8/13 11:38
 */
public class EmailAuthenticationProvider implements AuthenticationProvider {

    private final UserService userService;

    private final EmailVerificationService emailVerificationService;

    public EmailAuthenticationProvider(UserService userService, EmailVerificationService emailVerificationService){
        this.userService = userService;
        this.emailVerificationService = emailVerificationService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            return null;
        }
        EmailAuthenticationToken emailAuthenticationToken = (EmailAuthenticationToken) authentication;
        String email = (String) emailAuthenticationToken.getPrincipal();
        String inputCode = (String) emailAuthenticationToken.getCredentials();
        // 先校验验证码
        boolean verify = emailVerificationService.verify(email, inputCode, VerificationScene.LOGIN);
        if (!verify){
            throw new BadCredentialsException("验证码错误!");
        }
        Long userId = userService.ensureUserByIdentifier(IdentifierType.EMAIL, email);
        // 加载 UserDetails
        UserDetails userDetails = userService.loadUserByUserId(userId);
        if (userDetails == null) {
            throw new InternalAuthenticationServiceException("UserDetailsService returned null, which is an interface contract violation");
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
