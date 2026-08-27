package com.atlas.auth.config.security.authentication.provider;

import com.atlas.auth.config.security.mfa.*;
import com.atlas.auth.service.UserService;
import com.atlas.security.enums.ClientType;
import com.atlas.security.model.MfaType;
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

    private final MfaVerifyStrategyFactory mfaVerifyStrategyFactory;

    private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

    public MfaAuthenticationProvider(UserService userService,
                                     MfaTicketRepository mfaTicketRepository,
                                     MfaVerifyStrategyFactory mfaVerifyStrategyFactory) {
        this.userService = userService;
        this.mfaTicketRepository = mfaTicketRepository;
        this.mfaVerifyStrategyFactory = mfaVerifyStrategyFactory;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        MfaAuthenticationToken mfaAuthenticationToken = (MfaAuthenticationToken) authentication;
        String ticket = (String)mfaAuthenticationToken.getPrincipal();
        // 校验凭证是否存在或过期
        MfaChallenge mfaChallenge = mfaTicketRepository.load(ticket);
        if(mfaChallenge == null){
            throw new BadCredentialsException("MFA认证已失效，请重新登录");
        }
        // 认证方式
        MfaType mfaType = mfaAuthenticationToken.getMfaType();
        // 认证凭证
        MfaCredential credential = (MfaCredential) mfaAuthenticationToken.getCredentials();
        // 获取对应验证器
        MfaVerifyStrategy strategy = mfaVerifyStrategyFactory.getStrategy(mfaType);
        // 如果失败，策略内部会抛出异常
        strategy.verify(mfaChallenge, credential);
        // 立刻销毁 Ticket，防止重放轰炸
        mfaTicketRepository.remove(ticket);
        // 加载用户
        Long userId = mfaChallenge.getUserId();
        // 加载用户核心主体并校验状态
        UserDetails userDetails = userService.loadUserByUserId(userId);
        userDetailsChecker.check(userDetails);
        // 构建已认证的令牌
        MfaAuthenticationToken authenticated = MfaAuthenticationToken.authenticated(
                userDetails,
                null,
                mfaType,
                userDetails.getAuthorities()
        );
        authenticated.setDetails(mfaChallenge.getClientType());
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return MfaAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
