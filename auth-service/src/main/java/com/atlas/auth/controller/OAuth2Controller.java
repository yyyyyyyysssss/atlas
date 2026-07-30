package com.atlas.auth.controller;


import com.atlas.auth.config.security.oauth2.OidcUserInfoService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.security.model.SecurityUser;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/oauth2")
@Slf4j
public class OAuth2Controller {

    @Resource
    private OidcUserInfoService oidcUserInfoService;

    @GetMapping("/oidc/logout")
    public Result<?> oidcLogout(@RequestParam("state") String state) {
        log.info("oauth2 oidc logout code:{}", state);
        return ResultGenerator.ok();
    }


    @GetMapping("/v1/userinfo")
    public Result<Map<String,Object>> userinfo(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        Set<String> scopes = new HashSet<>(jwt.getClaimAsStringList("scope"));
        OidcUserInfo oidcUserInfo = oidcUserInfoService.loadUserByUserId(userId, scopes);
        return ResultGenerator.ok(oidcUserInfo.getClaims());
    }

}
