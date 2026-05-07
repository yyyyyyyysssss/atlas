package com.atlas.auth.controller;

import com.atlas.security.properties.SecurityProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping("/oauth2/activate")
    public String activate(@RequestParam(value = "user_code", required = false) String userCode) {
        log.info("user_code:{}", userCode);
        String uiUrl = securityProperties.getUiUrl();
        if (userCode != null) {
            return "redirect:" + uiUrl + "/oauth2/device_verification?user_code=" + userCode;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            String target = uiUrl + "/oauth2/activate";
            return "redirect:" + uiUrl + "/login?target=" + target;
        }
        return "redirect:" + uiUrl + "/oauth2/activate";
    }

}
