package com.atlas.auth.config.security.authentication.provider;

import com.atlas.auth.domain.dto.Web3WalletVerifySignatureDTO;
import com.atlas.auth.domain.dto.Web3WalletVerifySignatureResponse;
import com.atlas.auth.domain.entity.UserWeb3Credentials;
import com.atlas.auth.enums.IdentifierType;
import com.atlas.auth.service.UserService;
import com.atlas.auth.service.UserWeb3CredentialsService;
import com.atlas.auth.service.Web3WalletService;
import com.atlas.security.token.Web3WalletAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

import java.util.Optional;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/11 13:36
 */
@Slf4j
public class Web3WalletAuthenticationProvider implements AuthenticationProvider {

    private final UserService userService;

    private final Web3WalletService web3WalletService;

    private final UserWeb3CredentialsService userWeb3CredentialsService;

    private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

    public Web3WalletAuthenticationProvider(UserService userService, Web3WalletService web3WalletService, UserWeb3CredentialsService userWeb3CredentialsService) {
        this.userService = userService;
        this.web3WalletService = web3WalletService;
        this.userWeb3CredentialsService = userWeb3CredentialsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Web3WalletAuthenticationToken web3WalletAuthenticationToken = (Web3WalletAuthenticationToken) authentication;
        String web3Id = (String) web3WalletAuthenticationToken.getPrincipal();
        String signature = (String) web3WalletAuthenticationToken.getCredentials();
        Web3WalletVerifySignatureResponse verifySignatureResponse;
        try {
            verifySignatureResponse = web3WalletService.verifySignature(new Web3WalletVerifySignatureDTO(web3Id, signature));
        } catch (Exception e) {
            log.warn("Web3 钱包签名验证未通过, web3Id: {}, 原因: {}", web3Id, e.getMessage());
            throw new BadCredentialsException("Bad Credentials");
        }
        String address = verifySignatureResponse.address();
        Optional<UserWeb3Credentials> userWeb3Credentials = userWeb3CredentialsService.getByAddress(address);
        Long userId;
        // 不存在则静默创建
        if (userWeb3Credentials.isEmpty()) {
            userId = userService.ensureUserByIdentifier(IdentifierType.USERNAME, null);
            // 创建web3关联
            boolean created = userWeb3CredentialsService.saveCredential(
                    userId,
                    verifySignatureResponse.address(),
                    verifySignatureResponse.walletType(),
                    verifySignatureResponse.label(),
                    verifySignatureResponse.source()
            );
            if (!created) {
                log.warn("Web3 凭证创建关联失败,userId: {} address: {}", userId, address);
                throw new BadCredentialsException("Bad Credentials");
            }
        } else {
            userId = userWeb3Credentials.get().getUserId();
        }
        UserDetails userDetails = userService.loadUserByUserId(userId);
        if (userDetails == null) {
            throw new InternalAuthenticationServiceException("UserDetailsService returned null, which is an interface contract violation");
        }
        // 校验用户状态（是否被禁用、锁定等）
        userDetailsChecker.check(userDetails);
        // 构建已认证的 Token
        Web3WalletAuthenticationToken authenticated = Web3WalletAuthenticationToken.authenticated(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authenticated.setDetails(web3WalletAuthenticationToken.getDetails());
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {

        return Web3WalletAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
