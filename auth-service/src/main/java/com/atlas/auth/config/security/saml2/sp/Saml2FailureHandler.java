package com.atlas.auth.config.security.saml2.sp;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/12 14:34
 */
@Slf4j
public class Saml2FailureHandler implements AuthenticationFailureHandler {

    private final String uiUrl;

    public Saml2FailureHandler(String uiUrl){
        this.uiUrl = uiUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String errorMessage = "SAML2_AUTHENTICATION_FAILED";
        String detailMessage = exception.getMessage();
        if (exception instanceof Saml2AuthenticationException samlException) {
            errorMessage = samlException.getSaml2Error().getErrorCode();
            log.error("SAML2 协议层认证失败! 错误码: {}, 详细原因: {}", errorMessage, detailMessage);
        } else {
            log.error("SAML2 安全框架层认证失败! 原因: ", exception);
        }
        String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        String encodedDetail = URLEncoder.encode(detailMessage != null ? detailMessage : "", StandardCharsets.UTF_8);

        String targetUrl = String.format("%s/saml2/callback?error=%s&error_description=%s",uiUrl, encodedError, encodedDetail);

        log.warn("SAML2 认证彻底失败，正在将用户重定向回前端登录页: {}", targetUrl);
        response.sendRedirect(targetUrl);
    }
}
