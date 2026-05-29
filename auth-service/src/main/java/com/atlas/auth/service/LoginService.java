package com.atlas.auth.service;


import com.atlas.auth.domain.dto.*;
import com.atlas.security.enums.ClientType;
import com.atlas.security.model.SecurityUser;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.repository.SecurityContextStore;
import com.atlas.security.service.TokenService;
import com.atlas.security.token.CaptchaAuthenticationToken;
import com.atlas.security.token.RefreshAuthenticationToken;
import com.atlas.security.token.ThirdPartyAuthenticationToken;
import com.atlas.security.token.WebauthnAuthenticationToken;
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

    // 验证码/手机号登录
    public TokenResponse loginCaptcha(CaptchaLoginDTO captchaLoginDTO) {
        CaptchaAuthenticationToken captchaAuthenticationToken = new CaptchaAuthenticationToken(
                captchaLoginDTO.identity(),
                captchaLoginDTO.captcha(),
                captchaLoginDTO.captchaType().name()
        );
        return login(captchaAuthenticationToken, captchaLoginDTO.clientType(), true);
    }

    // 常规账密登录
    public TokenResponse loginPassword(PasswordLoginDTO passwordLoginDTO){
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                passwordLoginDTO.username(), passwordLoginDTO.password()
        );
        return login(usernamePasswordAuthenticationToken, passwordLoginDTO.clientType(), true);
    }

    // 凭证式一次性 Token 登录 (OTT)
    public TokenResponse loginOtt(OttLoginDTO ottLoginDTO){
        OneTimeTokenAuthenticationToken oneTimeTokenAuthenticationToken = new OneTimeTokenAuthenticationToken(
                ottLoginDTO.token()
        );
        return login(oneTimeTokenAuthenticationToken, ottLoginDTO.clientType(), true);
    }

    // FIDO2 / WebAuthn 生物特征或硬件密钥登录
    public TokenResponse loginWebauthn(WebauthnLoginDTO webauthnLoginDTO){
        WebauthnAuthenticationToken webauthnAuthenticationToken = new WebauthnAuthenticationToken(webauthnLoginDTO.webauthnAuthenticationRequest());
        return login(webauthnAuthenticationToken, webauthnLoginDTO.clientType(), true);
    }

    // 第三方 OAuth2 / 外部身份源导入登录
    public TokenResponse loginThirdParty(ThirdPartyLoginDTO thirdPartyLoginDTO){
        ThirdPartyAuthenticationToken thirdPartyAuthenticationToken = new ThirdPartyAuthenticationToken(thirdPartyLoginDTO.userId(), null);
        return login(thirdPartyAuthenticationToken, thirdPartyLoginDTO.clientType(), true);
    }

    /**
     * 核心多渠道收拢登录方法（模板方法模式）
     *
     * @param authenticationToken 各个渠道组装的、未认证的 Authentication 载体
     * @param clientType          客户端类型 (Web, App, MiniProgram) 用于做多端动态会话隔离
     * @param refresh             是否同步签发用于续期的 RefreshToken
     * @return 统一的 Token 响应体（可能包含最终真 Token，或者 MFA 拦截信号）
     */
    public TokenResponse login(Authentication authenticationToken, ClientType clientType, boolean refresh) {
        // 认证
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);
        SecurityUser securityUser = (SecurityUser) authenticate.getPrincipal();

        // 会话控制
        sessionControlService.kickOutExcessiveSessions(securityUser.getId(), clientType);

        // 发证
        TokenResponse tokenResponse = tokenService.createToken(securityUser, clientType, refresh);
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

    /**
     * 依托低频高安全的 RefreshToken 延长会话周期的核心刷新方法
     * 采用“滚动刷新(Token Rotation)”机制，每次调用都将物理作废旧凭证
     */
    public TokenResponse refreshToken(RefreshTokenDTO refreshTokenDTO) {
        RefreshAuthenticationToken refreshAuthenticationToken = new RefreshAuthenticationToken(
                refreshTokenDTO.token(),
                refreshTokenDTO.token()
        );
        Authentication authenticate = authenticationManager.authenticate(refreshAuthenticationToken);
        SecurityUser securityUser = (SecurityUser) authenticate.getPrincipal();

        // 发证
        TokenResponse tokenResponse = tokenService.createToken(securityUser, refreshTokenDTO.clientType(), true);
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
