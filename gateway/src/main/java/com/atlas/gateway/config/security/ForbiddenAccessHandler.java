package com.atlas.gateway.config.security;

import com.atlas.common.core.response.ResultCode;
import com.atlas.common.core.response.ResultGenerator;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 9:31
 */
public class ForbiddenAccessHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setHeader("Content-type", MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
        response.getWriter().println(ResultGenerator.failed(ResultCode.FORBIDDEN));
        response.getWriter().close();
    }

}
