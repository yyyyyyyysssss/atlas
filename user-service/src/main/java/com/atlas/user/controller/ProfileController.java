package com.atlas.user.controller;


import com.atlas.common.core.context.UserContext;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.user.domain.dto.UserProfileDTO;
import com.atlas.user.domain.vo.AuthInfoVO;
import com.atlas.user.domain.vo.OrgMemberVO;
import com.atlas.user.domain.vo.UserVO;
import com.atlas.user.enums.AuthorityDomain;
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

    @GetMapping
    public Result<UserVO> userInfo() {
        Long userId = UserContext.getRequiredUserId();
        UserVO userVO = profileService.userInfo(userId);
        return ResultGenerator.ok(userVO);
    }

    @GetMapping("/permissions")
    public Result<AuthInfoVO> getPermissions() {
        Long userId = UserContext.getRequiredUserId();
        AuthInfoVO authInfo = profileService.getPermissions(userId, AuthorityDomain.GLOBAL);
        return ResultGenerator.ok(authInfo);
    }

    // 获取我的团队成员
    @GetMapping("/team")
    public Result<List<OrgMemberVO>> getMyTeam() {
        Long userId = UserContext.getRequiredUserId();
        List<OrgMemberVO> myTeam = profileService.getMyTeam(userId);
        return ResultGenerator.ok(myTeam);
    }

    @PatchMapping
    public Result<?> profile(@RequestBody @Validated UserProfileDTO userProfileDTO) {
        Long userId = UserContext.getRequiredUserId();
        profileService.changeUserProfile(userId,userProfileDTO);
        return ResultGenerator.ok();
    }

}
