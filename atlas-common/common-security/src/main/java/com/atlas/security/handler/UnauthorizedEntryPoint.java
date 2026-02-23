package com.atlas.security.handler;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultCode;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.common.core.utils.JsonUtils;
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
        Result<Object> result = ResultGenerator.failed(ResultCode.UNAUTHORIZED);
        response.getWriter().println(JsonUtils.toJson(result));
        response.getWriter().flush();
    }

}
