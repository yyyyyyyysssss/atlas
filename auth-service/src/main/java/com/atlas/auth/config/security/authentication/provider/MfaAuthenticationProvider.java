package com.atlas.auth.config.security.authentication.provider;

import com.atlas.auth.config.security.mfa.MfaTicketContext;
import com.atlas.auth.config.security.mfa.MfaTicketRepository;
import com.atlas.auth.domain.entity.UserTotpCredentials;
import com.atlas.auth.service.TotpService;
import com.atlas.auth.service.UserService;
import com.atlas.auth.service.UserTotpCredentialsService;
import com.atlas.security.token.MfaAuthenticationToken;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

public class MfaAuthenticationProvider implements AuthenticationProvider {

    private final UserService userService;

    private final MfaTicketRepository mfaTicketRepository;

    private final UserTotpCredentialsService userTotpCredentialsService;

    private final TotpService totpService;

    private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

    public MfaAuthenticationProvider(UserService userService,
                                     MfaTicketRepository mfaTicketRepository,
                                     UserTotpCredentialsService userTotpCredentialsService,
                                     TotpService totpService) {
        this.userService = userService;
        this.mfaTicketRepository = mfaTicketRepository;
        this.userTotpCredentialsService = userTotpCredentialsService;
        this.totpService = totpService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        MfaAuthenticationToken mfaAuthenticationToken = (MfaAuthenticationToken) authentication;
        String ticket = (String)mfaAuthenticationToken.getPrincipal();
        // 校验凭证是否存在或过期
        MfaTicketContext mfaTicketContext = mfaTicketRepository.load(ticket);
        if(mfaTicketContext == null){
            throw new BadCredentialsException("登录凭证已过期，请重新登录");
        }

        Long userId = mfaTicketContext.getUserId();
        // 获取用户绑定的 TOTP 密钥
        UserTotpCredentials userTotpCredentials = userTotpCredentialsService.getActivatedByUserId(userId);
        if(userTotpCredentials == null){
            throw new BadCredentialsException("验证码错误或已失效");
        }
        String code = (String) mfaAuthenticationToken.getCredentials();
        boolean verify;
        try {
            // 即使 totpService 接收 Integer，也在这里安全转换，防止非数字引发 500 崩溃
            verify = totpService.verify(userTotpCredentials.getSecretKey(), Integer.parseInt(code));
        } catch (NumberFormatException e) {
            verify = false;
        }
        if(!verify){
            throw new BadCredentialsException("验证码错误或已失效");
        }
        // 立刻销毁 Ticket，防止重放轰炸
        mfaTicketRepository.remove(ticket);
        // 加载用户核心主体并校验状态
        UserDetails userDetails = userService.loadUserByUserId(userId);
        userDetailsChecker.check(userDetails);
        // 构建已认证的令牌
        MfaAuthenticationToken authenticated = MfaAuthenticationToken.authenticated(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authenticated.setDetails(authentication.getDetails());
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return MfaAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
