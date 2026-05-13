package com.atlas.gateway.config.security.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/13 9:31
 */
public class ApikeyAuthenticationFilter extends RequestHeaderAuthenticationFilter {

    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {

        return request.getRequestURI();
    }

    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {

        return request.getHeader("apikey");
    }

}
