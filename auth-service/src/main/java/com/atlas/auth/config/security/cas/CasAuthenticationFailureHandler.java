package com.atlas.auth.config.security.cas;

import com.atlas.common.core.response.ResultGenerator;
import com.atlas.common.core.utils.JsonUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/12 13:32
 */
public class CasAuthenticationFailureHandler implements AuthenticationFailureHandler {


    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        CasAuthenticationException casAuthenticationException = (CasAuthenticationException) exception;
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        response.getWriter().write(JsonUtils.toJson(ResultGenerator.failed(casAuthenticationException.getAuthErrorCode())));
    }
}
