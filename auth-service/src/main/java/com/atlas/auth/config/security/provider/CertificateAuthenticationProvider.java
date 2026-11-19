package com.atlas.auth.config.security.provider;

import com.atlas.auth.domain.dto.CertificateAuthenticateResponse;
import com.atlas.auth.domain.entity.UserCertificateCredentials;
import com.atlas.auth.service.CertificateService;
import com.atlas.auth.service.UserCertificateCredentialsService;
import com.atlas.auth.service.UserService;
import com.atlas.common.security.token.CertificateAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

import java.util.Objects;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/19 10:43
 */
@Slf4j
public class CertificateAuthenticationProvider implements AuthenticationProvider {

    private final CertificateService certificateService;

    private final UserService userService;

    private final UserCertificateCredentialsService userCertificateCredentialsService;

    private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

    public CertificateAuthenticationProvider(CertificateService certificateService, UserService userService, UserCertificateCredentialsService userCertificateCredentialsService) {
        this.certificateService = certificateService;
        this.userService = userService;
        this.userCertificateCredentialsService = userCertificateCredentialsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        CertificateAuthenticationToken certificateAuthenticationToken = (CertificateAuthenticationToken) authentication;
        String clientCertificate = certificateAuthenticationToken.getClientCertificate();
        String clientCertificateChain = certificateAuthenticationToken.getClientCertificateChain();
        try {
            // 验证客户端证书
            CertificateAuthenticateResponse authenticate = certificateService.authenticate(clientCertificate, clientCertificateChain);

            // 查询证书凭证
            UserCertificateCredentials credentials = userCertificateCredentialsService.findByFingerprint(authenticate.fingerprint());
            if (credentials == null) {
                log.warn("【证书认证】证书凭证不存在, fingerprint: {}", authenticate.fingerprint());
                throw new BadCredentialsException("Certificate authentication failed");
            }

            if (!Objects.equals(credentials.getUserId(), authenticate.userId())) {
                log.warn("【证书认证】证书凭证不属于该用户, fingerprint: {}", authenticate.fingerprint());
                throw new BadCredentialsException("Certificate authentication failed");
            }

            if (!Boolean.TRUE.equals(credentials.getEnabled())) {
                log.warn("【证书认证】证书凭证已禁用, fingerprint: {}", authenticate.fingerprint());
                throw new BadCredentialsException("Certificate authentication failed");
            }

            if (credentials.getRevokedTime() != null) {
                log.warn("【证书认证】证书凭证已撤销, fingerprint: {}", authenticate.fingerprint());
                throw new BadCredentialsException("Certificate authentication failed");
            }

            Long userId = authenticate.userId();
            UserDetails userDetails = userService.loadUserByUserId(userId);

            // 校验用户状态（是否被禁用、锁定等）
            userDetailsChecker.check(userDetails);

            return new CertificateAuthenticationToken(userDetails, userDetails.getAuthorities());
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("【证书认证】认证过程发生异常", e);
            throw new BadCredentialsException("Certificate authentication failed", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return CertificateAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
