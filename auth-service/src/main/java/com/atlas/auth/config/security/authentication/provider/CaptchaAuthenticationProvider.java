package com.atlas.auth.config.security.authentication.provider;

import com.atlas.auth.enums.CaptchaType;
import com.atlas.auth.enums.IdentifierType;
import com.atlas.auth.enums.SecurityScene;
import com.atlas.auth.service.CaptchaFactory;
import com.atlas.auth.service.CaptchaVerificationService;
import com.atlas.auth.service.UserService;
import com.atlas.security.token.CaptchaAuthenticationToken;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/20 10:16
 */
public class CaptchaAuthenticationProvider implements AuthenticationProvider {

    private final UserService userService;

    private final CaptchaFactory captchaFactory;

    private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

    public CaptchaAuthenticationProvider(UserService userService, CaptchaFactory captchaFactory){
        this.userService = userService;
        this.captchaFactory = captchaFactory;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            return null;
        }
        CaptchaAuthenticationToken captchaAuthenticationToken = (CaptchaAuthenticationToken) authentication;
        String principal = (String)captchaAuthenticationToken.getPrincipal();
        String captchaCode = (String) captchaAuthenticationToken.getCredentials();
        String captchaType = captchaAuthenticationToken.getCaptchaType();
        CaptchaType type;
        try {
            type = CaptchaType.valueOf(captchaType.trim().toUpperCase());
        } catch (Exception e) {
            throw new BadCredentialsException("不支持的验证码登录类型: " + captchaType);
        }
        CaptchaVerificationService captchaVerificationService = captchaFactory.getService(type);
        // 先校验验证码
        boolean verify = captchaVerificationService.verify(principal, captchaCode, SecurityScene.LOGIN);
        if (!verify){
            throw new BadCredentialsException("验证码错误!");
        }
        // 确保用户存在（不存在则自动创建，实现一键登录）
        Long userId = userService.ensureUserByIdentifier(IdentifierType.valueOf(captchaType), principal);
        // 加载 UserDetails
        UserDetails userDetails = userService.loadUserByUserId(userId);
        if (userDetails == null) {
            throw new InternalAuthenticationServiceException("UserDetailsService returned null, which is an interface contract violation");
        }
        // 校验用户状态（是否被禁用、锁定等）
        userDetailsChecker.check(userDetails);
        // 构建已认证的 Token
        CaptchaAuthenticationToken authenticated = CaptchaAuthenticationToken.authenticated(
                userDetails,
                null,
                captchaType,
                userDetails.getAuthorities()
        );
        authenticated.setDetails(captchaAuthenticationToken.getDetails());
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return CaptchaAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
