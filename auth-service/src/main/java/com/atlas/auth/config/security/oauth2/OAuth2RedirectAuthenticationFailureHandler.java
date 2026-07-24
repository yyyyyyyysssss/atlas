package com.atlas.auth.config.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * @Description
 * @Author ys
 * @Date 2026/7/24 10:38
 */
@Slf4j
public class OAuth2RedirectAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final String uiUrl;

    public OAuth2RedirectAuthenticationFailureHandler(String uiUrl) {
        this.uiUrl = uiUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.error("OAuth2 授权请求失败，准备重定向至前端错误页: {}", exception.getMessage());
        String error = "invalid_request";
        String message = "请求校验失败，请检查参数";
        String errorUri = null;

        if (exception instanceof OAuth2AuthenticationException oAuth2Exception) {
            OAuth2Error oAuth2Error = oAuth2Exception.getError();
            if (oAuth2Error != null) {
                // 对应前端 params.error (作为 title 渲染)
                error = oAuth2Error.getErrorCode();
                // 对应前端 params.message (作为 subTitle 渲染)
                message = StringUtils.hasText(oAuth2Error.getDescription())
                        ? oAuth2Error.getDescription()
                        : oAuth2Error.getErrorCode();
                // 对应前端 params.errorUri (作为 docUrl 排障文档链接渲染)
                errorUri = oAuth2Error.getUri();
            }
        }

        // 构建重定向 URL，精准匹配前端 ServerError 组件的入参结构
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uiUrl)
                .path("/500")
                .queryParam("error", error)
                .queryParam("message", message);

        if (StringUtils.hasText(errorUri)) {
            builder.queryParam("errorUri", errorUri);
        }

        String redirectUrl = builder.build().encode().toUriString();
        response.sendRedirect(redirectUrl);
    }
}
