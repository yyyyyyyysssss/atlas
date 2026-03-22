package com.atlas.user.controller;


import com.atlas.common.core.context.UserContext;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.user.domain.dto.ChangeAvatarDTO;
import com.atlas.user.domain.dto.ChangePasswordDTO;
import com.atlas.user.domain.dto.ShortcutUpdateDTO;
import com.atlas.user.domain.vo.AuthInfoVO;
import com.atlas.user.domain.vo.OrgMemberVO;
import com.atlas.user.domain.vo.UserInfoVO;
import com.atlas.user.domain.vo.UserVO;
import com.atlas.user.service.ProfileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Description 当前登录用户的个人信息管理控制器
 * @Author ys
 * @Date 2025/5/19 11:38
 */
@RequestMapping("/profile")
@RestController
@Slf4j
public class ProfileController {

    @Resource
    private ProfileService profileService;

    @GetMapping("/user/info")
    public Result<UserInfoVO> userInfo() {
        Long userId = UserContext.getRequiredUserId();
        UserInfoVO userInfoVO = profileService.userInfo(userId);
        return ResultGenerator.ok(userInfoVO);
    }

    @GetMapping("/auth/info")
    public Result<AuthInfoVO> authInfo() {
        Long userId = UserContext.getRequiredUserId();
        AuthInfoVO authInfo = profileService.authInfo(userId);
        return ResultGenerator.ok(authInfo);
    }

    // 获取我的团队成员
    @GetMapping("/team")
    public Result<List<OrgMemberVO>> getMyTeam() {
        Long userId = UserContext.getRequiredUserId();
        List<OrgMemberVO> myTeam = profileService.getMyTeam(userId);
        return ResultGenerator.ok(myTeam);
    }

    @PutMapping("/password")
    public Result<?> changePassword(@RequestBody @Validated ChangePasswordDTO changePasswordDTO) {
        Long userId = UserContext.getRequiredUserId();
        Boolean b = profileService.changePassword(userId, changePasswordDTO);
        return ResultGenerator.ok(b);
    }

    @PutMapping("/avatar")
    public Result<?> changeAvatar(@RequestBody @Validated ChangeAvatarDTO changeAvatarDTO) {
        Long userId = UserContext.getRequiredUserId();
        Boolean b = profileService.changeAvatar(userId, changeAvatarDTO.getNewAvatarUrl());
        return ResultGenerator.ok(b);
    }

    @PutMapping("/workbench/shortcuts")
    public Result<?> updateShortcuts(@RequestBody @Validated ShortcutUpdateDTO shortcutUpdateDTO) {
        Long userId = UserContext.getRequiredUserId();
        profileService.updateShortcuts(userId,shortcutUpdateDTO.getShortcuts());
        return ResultGenerator.ok();
    }

}
