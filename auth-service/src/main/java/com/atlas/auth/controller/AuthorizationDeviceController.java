package com.atlas.auth.controller;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.security.properties.SecurityProperties;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2024/7/30 9:49
 */
@Controller
@Slf4j
public class AuthorizationDeviceController {

    @Resource
    private SecurityProperties securityProperties;

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @GetMapping("/oauth2/activate")
    public String activate(@RequestParam(value = "user_code", required = false) String userCode) {
        log.info("user_code:{}", userCode);
        String uiUrl = securityProperties.getUiUrl();
        if (userCode != null) {
            return "redirect:" + uiUrl + "/oauth2/device_verification?user_code=" + userCode;
        }
        SecurityContext securityContext;
        if ((securityContext = SecurityContextHolder.getContext()) == null || securityContext.getAuthentication() == null || securityContext.getAuthentication() instanceof AnonymousAuthenticationToken) {
            String target = uiUrl + "/oauth2/activate";
            return "redirect:" + uiUrl + "/login?target=" + target;
        }
        return "redirect:" + uiUrl + "/oauth2/activate";
    }

    @GetMapping("/activated")
    public Result<Map<String,String>> activated() {
        String redirectUrl = securityProperties.getUiUrl() + "/oauth2/activated";
        return ResultGenerator.ok(Map.of(
                "redirectUrl",
                redirectUrl
        ));
    }

}
