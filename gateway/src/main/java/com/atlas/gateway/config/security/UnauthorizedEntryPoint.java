package com.atlas.gateway.config.security;

import com.atlas.common.core.response.ResultCode;
import com.atlas.common.core.response.ResultGenerator;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 9:28
 */
public class UnauthorizedEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("Content-type", MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
        response.getWriter().println(ResultGenerator.failed(ResultCode.UNAUTHORIZED));
        response.getWriter().close();
    }

}
