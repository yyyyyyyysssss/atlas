package com.atlas.auth.config.security.saml2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/12 13:32
 */
public class Saml2IdpAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final Saml2IdpService saml2IdpService;

    public Saml2IdpAuthenticationFailureHandler(Saml2IdpService saml2IdpService){
        this.saml2IdpService = saml2IdpService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        Saml2IdpAuthenticationException saml2IdpAuthenticationException = (Saml2IdpAuthenticationException)exception;
        String responseXml = saml2IdpService.createErrorResponse(saml2IdpAuthenticationException);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/saml+xml");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(responseXml);
        response.getWriter().flush();
    }
}
