package com.atlas.auth.controller;


import com.atlas.auth.domain.dto.*;
import com.atlas.auth.service.LoginService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.token.WebauthnAuthenticationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final LoginService loginService;

    // 账号密码登录
    @PostMapping("/login/password")
    public Result<TokenResponse> loginPassword(@RequestBody @Validated PasswordLoginDTO passwordLoginDTO){
        TokenResponse tokenResponse = loginService.loginPassword(passwordLoginDTO);
        return ResultGenerator.ok(tokenResponse);
    }

    // 验证码登录
    @PostMapping("/login/captcha")
    public Result<TokenResponse> loginCaptcha(@RequestBody @Validated CaptchaLoginDTO captchaLoginDTO){
        TokenResponse tokenResponse = loginService.loginCaptcha(captchaLoginDTO);
        return ResultGenerator.ok(tokenResponse);
    }

    // 一次令牌登录
    @PostMapping("/login/ott")
    public Result<TokenResponse> loginOtt(@RequestBody @Validated OttLoginDTO ottLoginDTO) {
        TokenResponse tokenResponse = loginService.loginOtt(ottLoginDTO);
        return ResultGenerator.ok(tokenResponse);
    }

    @PostMapping("/login/webauthn")
    public Result<TokenResponse> loginWebauthn(@RequestBody @Validated WebauthnLoginDTO webauthnLoginDTO) {
        TokenResponse tokenResponse = loginService.loginWebauthn(webauthnLoginDTO);
        return ResultGenerator.ok(tokenResponse);
    }

    @PostMapping("/login/mfa")
    public Result<TokenResponse> loginMfa(@RequestBody @Validated MfaLoginDTO mfaLoginDTO) {
        TokenResponse tokenResponse = loginService.loginMfa(mfaLoginDTO);
        return ResultGenerator.ok(tokenResponse);
    }

    @PostMapping("/refreshToken")
    public Result<TokenResponse> refreshToken(@RequestBody @Validated RefreshTokenDTO refreshTokenDTO) {
        TokenResponse tokenResponse = loginService.refreshToken(refreshTokenDTO);
        return ResultGenerator.ok(tokenResponse);
    }

}
