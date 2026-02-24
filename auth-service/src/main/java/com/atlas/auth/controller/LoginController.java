package com.atlas.auth.controller;


import com.atlas.auth.enums.LoginType;
import com.atlas.security.enums.ClientType;
import com.atlas.security.exception.TokenAuthenticationException;
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
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
                tokenResponse = loginService.login(authenticationToken, loginDTO.getClientType(),true, loginDTO.getRememberMe());
                break;
            case EMAIL:
                EmailAuthenticationToken emailAuthenticationToken = new EmailAuthenticationToken(loginDTO.getUsername(), loginDTO.getCredential());
                tokenResponse = loginService.login(emailAuthenticationToken, loginDTO.getClientType(), true,loginDTO.getRememberMe());
                break;
            case OTT:
                OneTimeTokenAuthenticationToken oneTimeTokenAuthenticationToken = new OneTimeTokenAuthenticationToken(loginDTO.getCredential());
                tokenResponse = loginService.login(oneTimeTokenAuthenticationToken, loginDTO.getClientType(), false, loginDTO.getRememberMe());
                break;
            case REMEMBER_ME:
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if(!(authentication instanceof RememberMeAuthenticationToken)){
                    throw new BadCredentialsException("非法认证请求");
                }
                tokenResponse = loginService.login(authentication, loginDTO.getClientType(),true, loginDTO.getRememberMe());
                break;
            default:
                throw new UnsupportedOperationException("不支持的登录方式:" + loginDTO.getLoginType());
        }
        if (tokenResponse == null) {
            throw new BadCredentialsException("Bad Credentials");
        }
        return ResultGenerator.ok(tokenResponse);
    }

    @GetMapping("/login/ott")
    public Result<?> login(@RequestParam("ottToken") String ottToken, @RequestParam(value = "clientType", required = false) ClientType clientType) {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setLoginType(LoginType.OTT);
        loginDTO.setCredential(ottToken);
        if (clientType == null) {
            clientType = ClientType.WEB;
        }
        loginDTO.setClientType(clientType);
        return login(loginDTO);
    }

    @PostMapping("/refreshToken")
    public Result<?> refreshToken(HttpServletRequest request) {
        String refreshToken = normalBearerTokenResolver.resolve(request);
        if (StringUtils.isEmpty(refreshToken)) {
            throw new TokenAuthenticationException("凭证不能为空");
        }
        TokenResponse tokenResponse = loginService.refreshToken(refreshToken);
        return ResultGenerator.ok(tokenResponse);
    }

}
