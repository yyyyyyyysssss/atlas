package com.atlas.auth.config.security.saml2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/10 17:47
 */
public class Saml2IdpAuthenticationConverter implements AuthenticationConverter {

    private Saml2IdpRequestContextRepository requestContextRepository;

    public Saml2IdpAuthenticationConverter(Saml2IdpRequestContextRepository requestContextRepository){
        this.requestContextRepository = requestContextRepository;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        String samlIssuer = request.getParameter("samlIssuer");
        String samlRequestId = request.getParameter("samlRequestId");
        if(samlIssuer == null || samlIssuer.isBlank() || samlRequestId == null || samlRequestId.isBlank()){
            return null;
        }
        Saml2IdpRequestContext saml2IdpRequestContext = requestContextRepository.consume(samlIssuer, samlRequestId);
        if(saml2IdpRequestContext == null){
            throw invalidRequest("SAML authentication context expired or invalid");
        }
        return new Saml2IdpAuthenticationToken(saml2IdpRequestContext);
    }

    public static Saml2IdpAuthenticationException invalidRequest(String description) {

        return new Saml2IdpAuthenticationException(new Saml2Error(Saml2ErrorCodes.INVALID_REQUEST, description));
    }
}
