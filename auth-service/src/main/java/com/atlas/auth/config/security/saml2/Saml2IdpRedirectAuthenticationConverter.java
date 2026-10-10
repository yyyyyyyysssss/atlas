package com.atlas.auth.config.security.saml2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/10 17:41
 */
public class Saml2IdpRedirectAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        return null;
    }


}
