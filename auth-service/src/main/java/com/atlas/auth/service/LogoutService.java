package com.atlas.auth.service;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.common.security.enums.ClientType;
import com.atlas.common.security.model.SecurityUser;
import com.atlas.common.security.service.TokenService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@Slf4j
public class LogoutService implements LogoutSuccessHandler {

    @Resource
    private TokenService tokenService;

    @Resource
    private SessionControlService sessionControlService;

    @Resource
    private CookieService cookieService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {


        if(authentication.getPrincipal() instanceof SecurityUser securityUser){
            ClientType clientType = securityUser.getClientType();
            // 撤销 Token
            tokenService.revoke(securityUser.getTokenId());
            // 移除会话
            sessionControlService.removeSession(securityUser.getId(),securityUser.getTokenId(),clientType);
            // 清除cookie
            if(ClientType.WEB.equals(clientType)){
                cookieService.clearAuthCookies(response);
            }
        }

        // 清理本地上下文
        SecurityContextHolder.clearContext();
        // 返回标准响应
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
        Result<Object> result = ResultGenerator.ok();
        response.getWriter().println(JsonUtils.toJson(result));
        response.getWriter().flush();
    }

    private void clearCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie
                .from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
