package com.atlas.auth.config.security.authentication.provider;

import com.atlas.auth.enums.VerificationScene;
import com.atlas.auth.service.EmailVerificationService;
import com.atlas.auth.service.UserDetailsServiceImpl;
import com.atlas.common.core.api.user.dto.ExternalIdentityDTO;
import com.atlas.common.core.api.user.dto.UserDTO;
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
        String email = (String) emailAuthenticationToken.getPrincipal();
        String inputCode = (String) emailAuthenticationToken.getCredentials();
        // 先校验验证码
        boolean verify = emailVerificationService.verify(email, inputCode, VerificationScene.LOGIN);
        if (!verify){
            throw new BadCredentialsException("验证码错误!");
        }
        // 身份供应 无论用户是否存在，ensureUser 都会返回一个有效的 UserDTO（不存在静默创建）
        ExternalIdentityDTO identity = new ExternalIdentityDTO();
        identity.setProvider("email");
        identity.setSub(email);
        identity.setFullName(email.split("@")[0]);
        identity.setEmail(email);
        UserDTO userDTO = ((UserDetailsServiceImpl) userDetailsService).ensureUser(identity);
        // 加载 UserDetails
        UserDetails userDetails = userDetailsService.loadUserByUsername(userDTO.getUsername());
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
