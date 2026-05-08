package com.atlas.auth.controller;


import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth2")
@Slf4j
public class OAuth2Controller {


    @GetMapping("/oidc/logout")
    public Result<?> oidcLogout(@RequestParam("state") String state) {
        log.info("oauth2 oidc logout code:{}", state);
        return ResultGenerator.ok();
    }

}
