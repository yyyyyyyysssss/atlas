package com.atlas.auth.controller;


import com.atlas.security.token.EmailAuthenticationToken;
import com.atlas.auth.domain.dto.LoginDTO;
import com.atlas.auth.service.LoginService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.resolver.NormalBearerTokenResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final LoginService loginService;

    private final NormalBearerTokenResolver normalBearerTokenResolver;

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<?> login(@RequestBody @Validated LoginDTO loginDTO) {
        TokenResponse tokenResponse;
        switch (loginDTO.getLoginType()) {
            case NORMAL:
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getCredential());
                tokenResponse = loginService.login(authenticationToken, loginDTO.getClientType(), loginDTO.rememberMe());
                break;
            case EMAIL:
                EmailAuthenticationToken emailAuthenticationToken = new EmailAuthenticationToken(loginDTO.getUsername(), loginDTO.getCredential());
                tokenResponse = loginService.login(emailAuthenticationToken, loginDTO.getClientType(), loginDTO.rememberMe());
                break;
            case OTT:
                OneTimeTokenAuthenticationToken oneTimeTokenAuthenticationToken = new OneTimeTokenAuthenticationToken(loginDTO.getCredential());
                tokenResponse = loginService.login(oneTimeTokenAuthenticationToken, loginDTO.getClientType(), loginDTO.rememberMe());
                break;
            default:
                throw new UnsupportedOperationException("不支持的登录方式:" + loginDTO.getLoginType());
        }
        if (tokenResponse == null) {
            throw new BadCredentialsException("Bad Credentials");
        }
        return ResultGenerator.ok(tokenResponse);
    }

    @PostMapping("/refreshToken")
    public Result<?> refreshToken(HttpServletRequest request) {
        String refreshToken = normalBearerTokenResolver.resolve(request);
        if (StringUtils.isEmpty(refreshToken)) {
            throw new BadCredentialsException("Bad Credentials");
        }
        TokenResponse tokenResponse = loginService.refreshToken(refreshToken);
        if (tokenResponse == null) {
            throw new BadCredentialsException("Bad Credentials");
        }
        return ResultGenerator.ok(tokenResponse);
    }

}
