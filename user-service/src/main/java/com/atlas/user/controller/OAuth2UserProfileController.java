package com.atlas.user.controller;

import com.atlas.common.core.context.UserContext;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.user.domain.entity.User;
import com.atlas.user.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/12 16:46
 */
@RestController
@RequestMapping("/v1/profile")
public class OAuth2UserProfileController {

    @Resource
    private UserService userService;

    @GetMapping("/info")
    public Result<Map<String, Object>> loadUser(){
        Long userId = UserContext.getRequiredUserId();
        User user = userService.getById(userId);
        OidcUserInfo oidcUserInfo = OidcUserInfo.builder()
                .subject(user.getId().toString())
                .preferredUsername(user.getUsername())
                .name(user.getFullName())
                .picture(user.getAvatar())
                .build();
        return ResultGenerator.ok(oidcUserInfo.getClaims());
    }

}
