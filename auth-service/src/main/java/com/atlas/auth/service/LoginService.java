package com.atlas.auth.service;


import com.atlas.auth.config.security.mfa.MfaTicketContext;
import com.atlas.auth.config.security.mfa.MfaTicketRepository;
import com.atlas.auth.domain.dto.*;
import com.atlas.security.enums.AuthAssuranceLevel;
import com.atlas.security.enums.ClientType;
import com.atlas.security.model.MfaType;
import com.atlas.security.model.SecurityUser;
import com.atlas.security.model.TokenInfo;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.repository.SecurityContextStore;
import com.atlas.security.service.TokenService;
import com.atlas.security.token.*;
import com.atlas.security.utils.TicketGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService {

    private final AuthenticationManager authenticationManager;

    private final SecurityContextStore securityContextStore;

    private final TokenService tokenService;

    private final SessionControlService sessionControlService;

    private final MfaTicketRepository mfaTicketRepository;

    // 验证码/手机号登录
    public TokenResponse loginCaptcha(CaptchaLoginDTO captchaLoginDTO) {
        CaptchaAuthenticationToken captchaAuthenticationToken = new CaptchaAuthenticationToken(
                captchaLoginDTO.identity(),
                captchaLoginDTO.captcha(),
                captchaLoginDTO.captchaType().name()
        );
        return login(captchaAuthenticationToken, captchaLoginDTO.clientType());
    }

    // 常规账密登录
    public TokenResponse loginPassword(PasswordLoginDTO passwordLoginDTO){
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                passwordLoginDTO.username(), passwordLoginDTO.password()
        );
        return login(usernamePasswordAuthenticationToken, passwordLoginDTO.clientType());
    }

    // 凭证式一次性 Token 登录 (OTT)
    public TokenResponse loginOtt(OttLoginDTO ottLoginDTO){
        OneTimeTokenAuthenticationToken oneTimeTokenAuthenticationToken = new OneTimeTokenAuthenticationToken(
                ottLoginDTO.token()
        );
        return login(oneTimeTokenAuthenticationToken, ottLoginDTO.clientType());
    }

    // FIDO2 / WebAuthn 生物特征或硬件密钥登录
    public TokenResponse loginWebauthn(WebauthnLoginDTO webauthnLoginDTO){
        WebauthnAuthenticationToken webauthnAuthenticationToken = new WebauthnAuthenticationToken(webauthnLoginDTO.webauthnAuthenticationRequest());
        return login(webauthnAuthenticationToken, webauthnLoginDTO.clientType());
    }

    // 第三方 OAuth2 / 外部身份源导入登录
    public TokenResponse loginThirdParty(ThirdPartyLoginDTO thirdPartyLoginDTO){
        ThirdPartyAuthenticationToken thirdPartyAuthenticationToken = new ThirdPartyAuthenticationToken(thirdPartyLoginDTO.userId(), null);
        return login(thirdPartyAuthenticationToken, thirdPartyLoginDTO.clientType());
    }


    public TokenResponse loginMfa(MfaLoginDTO mfaLoginDTO){
        MfaAuthenticationToken mfaAuthenticationToken = new MfaAuthenticationToken(mfaLoginDTO.ticket(), mfaLoginDTO.code(),mfaLoginDTO.mfaType());
        return login(mfaAuthenticationToken, null);
    }

    /**
     * 核心多渠道收拢登录方法（模板方法模式）
     *
     * @param authenticationToken 各个渠道组装的、未认证的 Authentication 载体
     * @param clientType          客户端类型 (Web, App, MiniProgram) 用于做多端动态会话隔离
     * @return 统一的 Token 响应体（可能包含最终真 Token，或者 MFA 拦截信号）
     */
    private TokenResponse login(Authentication authenticationToken, ClientType clientType) {
        // 认证
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);
        if (clientType == null && authenticate.getDetails() instanceof ClientType ct){
            clientType = ct;
        }
        if(clientType == null){
            throw new BadCredentialsException("客户端类型不能为空");
        }
        SecurityUser securityUser = (SecurityUser) authenticate.getPrincipal();

        // 确定当前用户账号被期望的安全信任等级：开启了 MFA 就是 MEDIUM，没开启则是 LOW
        AuthAssuranceLevel requiredLevel = securityUser.isMfaEnabled() ? AuthAssuranceLevel.MEDIUM : AuthAssuranceLevel.LOW;
        // 获取当前登录方式实际能够提供的安全信任等级
        AuthAssuranceLevel currentLevel = AuthAssuranceLevel.LOW; // 默认给最低，防止未实现接口的自定义Token钻空子
        if (authenticate instanceof AssuranceLevelAware awareToken) {
            currentLevel = awareToken.getAssuranceLevel();
        }
        // 如果当前登录方式的等级低于用户期望的等级，才触发 MFA 拦截
        if(currentLevel.getRank() < requiredLevel.getRank()){
            String ticket = TicketGenerator.generate();
            mfaTicketRepository.save(ticket,new MfaTicketContext(securityUser.getId(),clientType), Duration.ofMinutes(5));
            return TokenResponse.mfaRequired(ticket, MfaType.TOTP);
        }

        // 会话控制
        sessionControlService.kickOutExcessiveSessions(securityUser.getId(), clientType);

        return createToken(authenticate,clientType);
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

        // 移除旧会话
        String oldTokenId = ((RefreshAuthenticationToken) authenticate).getOldTokenId();
        sessionControlService.removeSession(securityUser.getId(),oldTokenId,refreshTokenDTO.clientType());

        return createToken(authenticate,refreshTokenDTO.clientType());
    }

    private TokenResponse createToken(Authentication authenticate, ClientType clientType){
        if(!authenticate.isAuthenticated()){
            throw new BadCredentialsException("Unauthenticated");
        }
        if (!(authenticate.getPrincipal() instanceof SecurityUser securityUser)) {
            throw new BadCredentialsException("Unsupported principal");
        }
        // 发证
        TokenInfo token = tokenService.createToken(securityUser, clientType, true);
        String tokenId = token.id();

        // 存储 (Context)
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authenticate);
        securityContextStore.saveContext(securityContext, tokenId);
        SecurityContextHolder.setContext(securityContext);

        // 注册会话
        sessionControlService.registerSession(securityUser.getId(), tokenId, token.access().expiresIn(), clientType);
        return TokenResponse.success(token);
    }

}
