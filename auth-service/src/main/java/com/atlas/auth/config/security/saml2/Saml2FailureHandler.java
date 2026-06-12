package com.atlas.auth.config.security.saml2;

import com.atlas.security.properties.SecurityProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/12 14:34
 */
@Component // 🌟 让 Spring 来 new 它，并自动把 securityProperties 塞给它
@Slf4j
public class Saml2FailureHandler implements AuthenticationFailureHandler {

    private final SecurityProperties securityProperties;

    public Saml2FailureHandler(SecurityProperties securityProperties){
        this.securityProperties = securityProperties;
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

        String uiUrl = securityProperties.getUiUrl();
        String targetUrl = String.format("%s/login/saml2/callback?error=%s&msg=%s",uiUrl, encodedError, encodedDetail);

        log.warn("SAML2 认证彻底失败，正在将用户重定向回前端登录页: {}", targetUrl);
        response.sendRedirect(targetUrl);
    }
}
