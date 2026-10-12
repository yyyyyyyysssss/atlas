package com.atlas.auth.config.security.provider;

import com.atlas.auth.config.security.saml2.Saml2IdpAuthenticationException;
import com.atlas.auth.config.security.saml2.Saml2IdpAuthenticationRequiredException;
import com.atlas.auth.config.security.saml2.Saml2IdpAuthenticationToken;
import com.atlas.auth.config.security.saml2.Saml2IdpRequestContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/12 15:06
 */
public class Saml2IdpAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Saml2IdpAuthenticationToken saml2IdpAuthenticationToken = (Saml2IdpAuthenticationToken)authentication;
        Saml2IdpRequestContext saml2IdpRequestContext = saml2IdpAuthenticationToken.getSaml2IdpRequestContext();
        if(saml2IdpRequestContext == null){
            throw invalidRequest("Missing SAML request context");
        }
        // 判断是否已认证
        Authentication currentAuthentication  = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = currentAuthentication != null
                && currentAuthentication.isAuthenticated()
                && !(currentAuthentication instanceof AnonymousAuthenticationToken);
        if(!authenticated){
            throw new Saml2IdpAuthenticationRequiredException(saml2IdpRequestContext.getSpEntityId(), saml2IdpRequestContext.getRequestId());
        }
        return new Saml2IdpAuthenticationToken(currentAuthentication.getPrincipal(), saml2IdpRequestContext, currentAuthentication.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return Saml2IdpAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public static Saml2IdpAuthenticationException invalidRequest(String description) {

        return new Saml2IdpAuthenticationException(new Saml2Error(Saml2ErrorCodes.INVALID_REQUEST, description));
    }
}
