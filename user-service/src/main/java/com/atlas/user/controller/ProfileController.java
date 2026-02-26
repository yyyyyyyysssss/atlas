package com.atlas.user.controller;


import com.atlas.common.core.context.UserContext;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.user.domain.dto.ChangeAvatarDTO;
import com.atlas.user.domain.dto.ChangePasswordDTO;
import com.atlas.user.domain.vo.UserInfoVO;
import com.atlas.user.service.ProfileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    public Result<?> userInfo() {
        Long userId = UserContext.getRequiredUserId();
        UserInfoVO userInfoVO = profileService.userInfo(userId);
        return ResultGenerator.ok(userInfoVO);
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

}
