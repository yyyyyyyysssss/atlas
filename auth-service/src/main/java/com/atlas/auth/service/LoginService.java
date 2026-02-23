package com.atlas.auth.service;


import com.atlas.security.enums.ClientType;
import com.atlas.security.model.SecurityUser;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.repository.SecurityContextStore;
import com.atlas.security.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService {

    private final AuthenticationManager authenticationManager;

    private final SecurityContextStore securityContextStore;

    private final TokenService tokenService;

    public TokenResponse login(Authentication authenticationToken, ClientType clientType) {

        return login(authenticationToken, clientType, false);
    }

    public TokenResponse login(Authentication authenticationToken, ClientType clientType, boolean rememberMe) {
        // 执行认证
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);
        // 类型安全检查
        if (!(authenticate.getPrincipal() instanceof SecurityUser securityUser)) {
            throw new InternalAuthenticationServiceException("认证系统内部错误：无法获取用户信息主体");
        }
        //生成token
        TokenResponse tokenResponse = tokenService.createToken(securityUser, clientType, rememberMe);
        String tokenId = tokenResponse.tokenId();
        securityUser.setTokenId(tokenId);
        //序列化securityContext
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authenticate);
        securityContextStore.saveContext(securityContext, tokenId);
        // 更新当前线程的 Holder
        SecurityContextHolder.setContext(securityContext);
        return tokenResponse;
    }

    public TokenResponse refreshToken(String refreshToken) {
        return tokenService.refreshToken(refreshToken);
    }

}
