package com.atlas.auth.service;


import com.atlas.auth.domain.dto.CaptchaLoginDTO;
import com.atlas.auth.domain.dto.OttLoginDTO;
import com.atlas.auth.domain.dto.PasswordLoginDTO;
import com.atlas.auth.domain.dto.RefreshTokenDTO;
import com.atlas.security.enums.ClientType;
import com.atlas.security.model.SecurityUser;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.repository.SecurityContextStore;
import com.atlas.security.service.TokenService;
import com.atlas.security.token.CaptchaAuthenticationToken;
import com.atlas.security.token.RefreshAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
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

    public TokenResponse loginCaptcha(CaptchaLoginDTO captchaLoginDTO) {
        CaptchaAuthenticationToken captchaAuthenticationToken = new CaptchaAuthenticationToken(
                captchaLoginDTO.identity(),
                captchaLoginDTO.captcha(),
                captchaLoginDTO.captchaType().name()
        );
        return login(captchaAuthenticationToken, captchaLoginDTO.clientType(), true, false);
    }

    public TokenResponse loginPassword(PasswordLoginDTO passwordLoginDTO){
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                passwordLoginDTO.username(), passwordLoginDTO.password()
        );
        return login(usernamePasswordAuthenticationToken, passwordLoginDTO.clientType(), true, false);
    }

    public TokenResponse loginOtt(OttLoginDTO ottLoginDTO){
        OneTimeTokenAuthenticationToken oneTimeTokenAuthenticationToken = new OneTimeTokenAuthenticationToken(
                ottLoginDTO.token()
        );
        return login(oneTimeTokenAuthenticationToken, ottLoginDTO.clientType(), true, false);
    }

    public TokenResponse login(Authentication authenticationToken, ClientType clientType, boolean refresh, boolean rememberMe) {
        // 认证
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);
        SecurityUser securityUser = (SecurityUser) authenticate.getPrincipal();

        // 会话控制
        sessionControlService.kickOutExcessiveSessions(securityUser.getId(), clientType);

        // 发证
        TokenResponse tokenResponse = tokenService.createToken(securityUser, clientType, refresh, rememberMe);
        String tokenId = tokenResponse.tokenId();

        // 存储 (Context)
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authenticate);
        securityContextStore.saveContext(securityContext, tokenId);
        SecurityContextHolder.setContext(securityContext);

        // 注册会话
        sessionControlService.registerSession(securityUser.getId(), tokenId, tokenResponse.access().expiresIn(), clientType);
        return tokenResponse;
    }

    public TokenResponse refreshToken(RefreshTokenDTO refreshTokenDTO) {
        RefreshAuthenticationToken refreshAuthenticationToken = new RefreshAuthenticationToken(
                refreshTokenDTO.token(),
                refreshTokenDTO.token()
        );
        Authentication authenticate = authenticationManager.authenticate(refreshAuthenticationToken);
        SecurityUser securityUser = (SecurityUser) authenticate.getPrincipal();

        // 发证
        TokenResponse tokenResponse = tokenService.createToken(securityUser, refreshTokenDTO.clientType(), true, false);
        String tokenId = tokenResponse.tokenId();

        // 存储 (Context)
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authenticate);
        securityContextStore.saveContext(securityContext, tokenId);
        SecurityContextHolder.setContext(securityContext);

        // 移除旧会话
        String oldTokenId = ((RefreshAuthenticationToken) authenticate).getOldTokenId();
        sessionControlService.removeSession(securityUser.getId(),oldTokenId,refreshTokenDTO.clientType());

        // 注册会话
        sessionControlService.registerSession(securityUser.getId(), tokenId, tokenResponse.access().expiresIn(), refreshTokenDTO.clientType());

        return tokenResponse;
    }

}
