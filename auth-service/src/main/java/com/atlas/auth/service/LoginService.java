package com.atlas.auth.service;


import com.atlas.security.enums.ClientType;
import com.atlas.security.enums.TokenType;
import com.atlas.security.exception.TokenAuthenticationException;
import com.atlas.security.model.PayloadInfo;
import com.atlas.security.model.SecurityUser;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.repository.SecurityContextStore;
import com.atlas.security.service.TokenService;
import com.atlas.security.token.RefreshAuthenticationToken;
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

    private final SessionControlService sessionControlService;

    public TokenResponse login(Authentication authenticationToken, ClientType clientType, boolean refresh, boolean rememberMe) {
        // 认证
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);
        SecurityUser securityUser = (SecurityUser) authenticate.getPrincipal();

        // 会话控制
        sessionControlService.kickOutExcessiveSessions(securityUser.getId());

        // 发证
        TokenResponse tokenResponse = tokenService.createToken(securityUser, clientType, refresh, rememberMe);
        String tokenId = tokenResponse.tokenId();

        // 存储 (Context)
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authenticate);
        securityContextStore.saveContext(securityContext, tokenId);
        SecurityContextHolder.setContext(securityContext);

        // 注册会话
        sessionControlService.registerSession(securityUser.getId(),tokenId,tokenResponse.access().expiresIn());
        return tokenResponse;
    }

    public TokenResponse refreshToken(String refreshToken) {
        PayloadInfo payloadInfo = tokenService.verify(refreshToken, TokenType.REFRESH_TOKEN);
        if (payloadInfo == null) {
            throw new TokenAuthenticationException("刷新令牌已失效");
        }
        String userId = payloadInfo.getSubject();
        RefreshAuthenticationToken refreshAuthenticationToken = new RefreshAuthenticationToken(userId, null);
        return login(refreshAuthenticationToken, payloadInfo.getClientType(), true, false);
    }

}
