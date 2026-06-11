package com.atlas.auth.controller;

import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.vo.*;
import com.atlas.auth.enums.SecurityScene;
import com.atlas.auth.service.AccountService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.security.model.SecurityUser;
import com.atlas.security.token.WebauthnAuthenticationRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/21 11:55
 */
@RequestMapping("/account")
@RestController
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;


    /**
     * 获取账号安全与绑定状态
     */
    @GetMapping("/security")
    public Result<AccountSecurityVO> accountSecurity(@AuthenticationPrincipal SecurityUser securityUser) {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        Object principal = authentication.getPrincipal();
        log.info("principal: {}", principal);
        AccountSecurityVO accountSecurity = accountService.getAccountSecurity(securityUser.getId());
        return ResultGenerator.ok(accountSecurity);
    }

    /**
     * 修改/设置用户名
     */
    @PutMapping("/username")
    public Result<Void> changeUsername(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated ChangeUsernameDTO changeUsernameDTO) {
        accountService.changeUsername(securityUser.getId(), changeUsernameDTO);
        return ResultGenerator.ok();
    }

    /**
     * 初始化/设置密码（仅限未设置密码时调用）
     */
    @PutMapping("/password/init")
    public Result<Void> initPassword(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated InitPasswordDTO initPasswordDTO) {
        accountService.initPassword(securityUser.getId(), initPasswordDTO);
        return ResultGenerator.ok();
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated ChangePasswordDTO changePasswordDTO) {
        accountService.changePassword(securityUser.getId(), changePasswordDTO);
        return ResultGenerator.ok();
    }

    @PostMapping("/password/verify")
    public Result<VerifyPasswordVO> verifyPassword(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated VerifyPasswordDTO verifyPasswordDTO) {
        VerifyPasswordVO verifyPasswordVO = accountService.verifyPassword(securityUser.getId(), verifyPasswordDTO);
        return ResultGenerator.ok(verifyPasswordVO);
    }

    @PostMapping("/captcha/verify")
    public Result<VerifyCaptchaVO> verifyCaptcha(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated CaptchaVerifyDTO captchaVerifyDTO) {
        VerifyCaptchaVO verifyCaptchaVO = accountService.verifyCaptcha(securityUser.getId(), captchaVerifyDTO);
        return ResultGenerator.ok(verifyCaptchaVO);
    }

    @PutMapping("/email/init")
    public Result<Void> initEmail(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated InitEmailDTO initEmailDTO) {
        accountService.initEmail(securityUser.getId(), initEmailDTO);
        return ResultGenerator.ok();
    }

    /**
     * 修改邮箱
     */
    @PutMapping("/email")
    public Result<Void> changeEmail(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated ChangeEmailDTO changeEmailDTO) {
        accountService.changeEmail(securityUser.getId(), changeEmailDTO);
        return ResultGenerator.ok();
    }

    @PostMapping("/webauthn/register")
    public Result<WebauthnRegistrationResponse> registerWebauthn(HttpServletRequest request,  @AuthenticationPrincipal SecurityUser securityUser, @RequestBody WebAuthnRegistrationRequest webAuthnRegistrationRequest){
        WebauthnRegistrationResponse response = accountService.registerWebauthn(request, securityUser.getId(),webAuthnRegistrationRequest);
        return ResultGenerator.ok(response);
    }

    @PostMapping("/webauthn/verify")
    public Result<VerifyWebauthnVO> verifyWebauthn(@AuthenticationPrincipal SecurityUser securityUser,
                                                   @RequestBody WebauthnAuthenticationRequest webauthnAuthenticationRequest,
                                                   @RequestParam("securityScene") String securityScene) {
        VerifyWebauthnVO verifyWebauthnVO = accountService.verifyWebauthn(securityUser.getId(), webauthnAuthenticationRequest, SecurityScene.fromString(securityScene));
        return ResultGenerator.ok(verifyWebauthnVO);
    }

    @DeleteMapping("/webauthn/unbind")
    public Result<Void> unbindWebauthn(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated UnbindWebauthnDTO unbindWebauthnDTO) {
        accountService.unbindWebauthn(securityUser.getId(), unbindWebauthnDTO);
        return ResultGenerator.ok();
    }

    // 验证 TOTP 动态验证码
    @PostMapping("/totp/verify")
    public Result<TotpVerifyVO> verifyTotp(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated TotpVerifyDTO totpVerifyDTO) {
        TotpVerifyVO verifyTotpVO = accountService.verifyTotp(securityUser.getId(), totpVerifyDTO);
        return ResultGenerator.ok(verifyTotpVO);
    }

    // 申请绑定/重置 TOTP 凭证
    @PostMapping("/totp")
    public Result<TotpInitVO> initTotp(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated TotpInitDTO totpInitDTO) {
        TotpInitVO totpInitVO = accountService.initTotp(securityUser.getId(),totpInitDTO);
        return ResultGenerator.ok(totpInitVO);
    }

    // 验证并激活
    @PutMapping("/totp")
    public Result<TotpActivateVO> activateTotp(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody TotpActivateDTO totpActivateDTO) {
        TotpActivateVO totpActivateVO = accountService.activateTotp(securityUser.getId(), totpActivateDTO);
        return ResultGenerator.ok(totpActivateVO);
    }

    // 解绑
    @DeleteMapping("/totp")
    public Result<Void> unbindTotp(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody TotpUnbindDTO totpUnbindDTO) {
        accountService.unbindTotp(securityUser.getId(), totpUnbindDTO);
        return ResultGenerator.ok();
    }

    @PostMapping("/backupCode/refresh")
    public Result<MfaRefreshBackupCodeVO> refreshTotpBackupCode(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated MfaRefreshBackupCodeDTO mfaRefreshBackupCodeDTO) {
        MfaRefreshBackupCodeVO mfaRefreshBackupCodeVO = accountService.refreshTotpBackupCode(securityUser.getId(), mfaRefreshBackupCodeDTO);
        return ResultGenerator.ok(mfaRefreshBackupCodeVO);
    }

    // 绑定或修改（重置）手势凭证
    @PostMapping("/gesture")
    public Result<Void> bindGesture(@AuthenticationPrincipal SecurityUser securityUser,@RequestBody @Validated GestureBindDTO gestureBindDTO) {
        accountService.bindGesture(securityUser.getId(),gestureBindDTO);
        return ResultGenerator.ok();
    }

    // 解绑手势凭证
    @DeleteMapping("/gesture")
    public Result<Void> unbindGesture(@AuthenticationPrincipal SecurityUser securityUser,@RequestBody @Validated GestureUnbindDTO gestureUnbindDTO) {
        accountService.unbindGesture(securityUser.getId(),gestureUnbindDTO);
        return ResultGenerator.ok();
    }

    // 验证 手势 凭证
    @PostMapping("/gesture/verify")
    public Result<GestureVerifyVO> verifyGesture(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated GestureVerifyDTO gestureVerifyDTO) {
        GestureVerifyVO gestureVerifyVO = accountService.verifyGesture(securityUser.getId(), gestureVerifyDTO);
        return ResultGenerator.ok(gestureVerifyVO);
    }


    @PostMapping("/web3/wallet")
    public Result<Void> bindWeb3Wallet(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated Web3WalletBindDTO web3WalletBindDTO){
        accountService.bindWeb3Wallet(securityUser.getId(), web3WalletBindDTO);
        return ResultGenerator.ok();
    }

    @PostMapping("/web3/wallet/verify")
    public Result<Web3WalletVerifyVO> verifyWeb3Wallet(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated Web3WalletVerifyDTO web3WalletVerifyDTO){
        Web3WalletVerifyVO web3WalletVerifyVO = accountService.verifyWeb3Wallet(securityUser.getId(), web3WalletVerifyDTO);
        return ResultGenerator.ok(web3WalletVerifyVO);
    }

    @DeleteMapping("/web3/wallet")
    public Result<Void> unbindWeb3Wallet(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated Web3WalletUnbindDTO web3WalletUnbindDTO){
        accountService.unbindWeb3Wallet(securityUser.getId(), web3WalletUnbindDTO);
        return ResultGenerator.ok();
    }

}
