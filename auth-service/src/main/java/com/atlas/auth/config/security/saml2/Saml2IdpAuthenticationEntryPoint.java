package com.atlas.auth.config.security.saml2;

import com.atlas.common.security.properties.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/12 9:16
 */
public class Saml2IdpAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityProperties securityProperties;

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public Saml2IdpAuthenticationEntryPoint(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        if (!(authException instanceof Saml2IdpAuthenticationRequiredException e)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        String targetUrl = "/saml2/idp/sso?samlIssuer="
                + URLEncoder.encode(e.getIssuer(), StandardCharsets.UTF_8)
                + "&samlRequestId="
                + URLEncoder.encode(e.getRequestId(), StandardCharsets.UTF_8);

        String loginUrl = securityProperties.getUiUrl()
                + "/login?targetUrl="
                + URLEncoder.encode(targetUrl, StandardCharsets.UTF_8);

        this.redirectStrategy.sendRedirect(request, response, loginUrl);
    }
}
