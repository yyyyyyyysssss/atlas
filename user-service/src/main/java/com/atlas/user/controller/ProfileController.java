package com.atlas.user.controller;


import com.atlas.common.core.api.user.dto.UserProfileDTO;
import com.atlas.common.core.context.UserContext;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.user.domain.dto.ChangeUserProfileDTO;
import com.atlas.user.domain.vo.OrgMemberVO;
import com.atlas.user.domain.vo.PermissionsInfoVO;
import com.atlas.user.enums.AuthorityDomain;
import com.atlas.user.service.ProfileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
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
    public Result<UserProfileDTO> userProfile() {
        Long userId = UserContext.getRequiredUserId();
        UserProfileDTO userProfile = profileService.userProfile(userId);
        return ResultGenerator.ok(userProfile);
    }

    @GetMapping("/permissions")
    public Result<PermissionsInfoVO> getPermissions(@RequestParam(name = "domain", required = false) String domain) {
        Long userId = UserContext.getRequiredUserId();
        AuthorityDomain authorityDomain = StringUtils.hasText(domain) ? AuthorityDomain.fromString(domain) : AuthorityDomain.GLOBAL;
        PermissionsInfoVO authInfo = profileService.getPermissions(userId, authorityDomain);
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
    public Result<?> changeProfile(@RequestBody @Validated ChangeUserProfileDTO userProfileDTO) {
        Long userId = UserContext.getRequiredUserId();
        profileService.changeUserProfile(userId,userProfileDTO);
        return ResultGenerator.ok();
    }

}
