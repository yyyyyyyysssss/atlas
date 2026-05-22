package com.atlas.auth.controller;

import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.vo.AccountSecurityVO;
import com.atlas.auth.service.AccountService;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.security.model.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
        AccountSecurityVO accountSecurity = accountService.getAccountSecurity(securityUser.getId());
        return ResultGenerator.ok(accountSecurity);
    }

    /**
     * 修改/设置用户名
     */
    @PutMapping("/username")
    public Result<Void> changeUsername(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated ChangeUsernameDTO changeUsernameDTO){
        accountService.changeUsername(securityUser.getId(),changeUsernameDTO);
        return ResultGenerator.ok();
    }

    /**
     * 初始化/设置密码（仅限未设置密码时调用）
     */
    @PutMapping("/init/password")
    public Result<Void> initPassword(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated InitPasswordDTO initPasswordDTO){
        accountService.initPassword(securityUser.getId(),initPasswordDTO);
        return ResultGenerator.ok();
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated ChangePasswordDTO changePasswordDTO){
        if(changePasswordDTO.verifyMethod().equals("change") && StringUtils.isBlank(changePasswordDTO.oldPassword())){
            throw new BusinessException("原密码不能为空");
        }
        if(changePasswordDTO.verifyMethod().equals("reset") && StringUtils.isBlank(changePasswordDTO.code())){
            throw new BusinessException("验证码不能为空");
        }
        accountService.changePassword(securityUser,changePasswordDTO);
        return ResultGenerator.ok();
    }

    @PostMapping("/verify/password")
    public Result<Boolean> verifyPassword(@AuthenticationPrincipal SecurityUser securityUser,@RequestBody @Validated VerifyPasswordDTO verifyPasswordDTO){
        boolean verify = accountService.verifyPassword(securityUser.getId(), verifyPasswordDTO);
        return ResultGenerator.ok(verify);
    }

    /**
     * 修改邮箱
     */
    @PutMapping("/email")
    public Result<Void> changeEmail(@AuthenticationPrincipal SecurityUser securityUser, @RequestBody @Validated ChangeEmailDTO changeEmailDTO){
        accountService.changeEmail(securityUser.getId(), changeEmailDTO);
        return ResultGenerator.ok();
    }


}
