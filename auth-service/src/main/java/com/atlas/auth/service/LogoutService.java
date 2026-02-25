package com.atlas.auth.service;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.security.model.PayloadInfo;
import com.atlas.security.resolver.NormalBearerTokenResolver;
import com.atlas.security.service.TokenService;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class LogoutService implements LogoutSuccessHandler {

    @Resource
    private NormalBearerTokenResolver normalBearerTokenResolver;

    @Resource
    private TokenService tokenService;

    @Resource
    private SessionControlService sessionControlService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String token = normalBearerTokenResolver.resolve(request);
        if (token == null) {
            return;
        }
        String tokenId = tokenService.extractInfo(token, PayloadInfo::getId);
        String userId = tokenService.extractInfo(token, PayloadInfo::getSubject);
        tokenService.revoke(token);
        // 移除会话
        sessionControlService.removeSession(Long.parseLong(userId),tokenId);
        // 清理本地上下文
        SecurityContextHolder.clearContext();
        // 返回标准响应
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Content-type", MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
        Result<Object> result = ResultGenerator.ok();
        response.getWriter().println(JsonUtils.toJson(result));
        response.getWriter().flush();
    }
}
