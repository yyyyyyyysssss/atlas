package com.atlas.auth.config.security.provider;

import com.atlas.auth.domain.dto.WebauthnAuthenticateResponse;
import com.atlas.auth.domain.entity.UserWebauthnCredentials;
import com.atlas.auth.service.UserService;
import com.atlas.auth.service.UserWebauthnCredentialsService;
import com.atlas.auth.service.WebauthnService;
import com.atlas.common.security.token.WebauthnAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.util.Assert;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/26 14:57
 */
@Slf4j
public class WebauthnAuthenticationProvider implements AuthenticationProvider {

    private final WebauthnService webauthnService;

    private final UserService userService;

    private final UserWebauthnCredentialsService userWebauthnCredentialsService;

    private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

    public WebauthnAuthenticationProvider(WebauthnService webauthnService, UserService userService, UserWebauthnCredentialsService userWebauthnCredentialsService) {
        Assert.notNull(webauthnService, "webauthnService cannot be null");
        Assert.notNull(userService, "userService cannot be null");
        this.webauthnService = webauthnService;
        this.userService = userService;
        this.userWebauthnCredentialsService = userWebauthnCredentialsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        WebauthnAuthenticationToken webauthnAuthenticationToken = (WebauthnAuthenticationToken) authentication;
        try {
            WebauthnAuthenticateResponse authenticate = this.webauthnService.authenticate(webauthnAuthenticationToken.getWebAuthnRequest());

            // 校验凭据是否存在且启用
            UserWebauthnCredentials userWebauthnCredentials = userWebauthnCredentialsService.findByCredentialId(authenticate.credentialId());
            if (userWebauthnCredentials == null) {
                log.warn("【通行密钥认证】该凭证不存在, credentialId: {}", authenticate.credentialId());
                throw new BadCredentialsException("Webauthn authentication failed");
            }

            if (Boolean.FALSE.equals(userWebauthnCredentials.getEnabled())) {
                log.warn("【通行密钥认证】该凭证已禁用, credentialId: {}", authenticate.credentialId());
                throw new BadCredentialsException("Webauthn authentication failed");
            }

            // 校验凭据所属用户
            Long userId = authenticate.userId();
            if (!userWebauthnCredentials.getUserId().equals(userId)) {
                log.warn("【通行密钥认证】该通行密钥不属于该用户, credentialId: {}", authenticate.credentialId());
                throw new BadCredentialsException("Webauthn authentication failed");
            }

            // 加载用户
            UserDetails userDetails = userService.loadUserByUserId(userId);

            // 校验用户状态（是否被禁用、锁定等）
            userDetailsChecker.check(userDetails);

            return new WebauthnAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("【通行密钥认证】认证过程发生异常", e);
            throw new BadCredentialsException("Webauthn authentication failed", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return WebauthnAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
