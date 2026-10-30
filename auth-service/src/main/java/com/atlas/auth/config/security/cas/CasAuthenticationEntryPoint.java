package com.atlas.auth.config.security.cas;

import com.atlas.common.security.properties.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/12 9:16
 */
public class CasAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityProperties securityProperties;

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public CasAuthenticationEntryPoint(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        if (!(authException instanceof CasAuthenticationRequiredException requiredException)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        String loginUrl = UriComponentsBuilder
                .fromUriString(securityProperties.getUiUrl() + "/login")
                .queryParam("casRequestId", requiredException.getCasRequestId())
                .build()
                .encode()
                .toUriString();

        this.redirectStrategy.sendRedirect(request, response, loginUrl);
    }
}
