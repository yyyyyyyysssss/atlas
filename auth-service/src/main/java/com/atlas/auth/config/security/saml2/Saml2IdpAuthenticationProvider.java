package com.atlas.auth.config.security.saml2;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class Saml2IdpAuthenticationProvider implements AuthenticationProvider {

    private final Saml2IdpRequestContextRepository saml2IdpRequestContextRepository;

    public Saml2IdpAuthenticationProvider(Saml2IdpRequestContextRepository saml2IdpRequestContextRepository) {
        this.saml2IdpRequestContextRepository = saml2IdpRequestContextRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Saml2IdpAuthenticationToken saml2IdpAuthenticationToken = (Saml2IdpAuthenticationToken) authentication;
        Saml2IdpRequestContext saml2IdpRequestContext = saml2IdpAuthenticationToken.getSaml2IdpRequestContext();
        if (saml2IdpRequestContext == null) {
            throw invalidRequest("Missing SAML request context");
        }

        String spEntityId = saml2IdpRequestContext.getSpEntityId();
        String requestId = saml2IdpRequestContext.getRequestId();
        // 是否强制重新认证 先不做处理 后续增加重新认证再处理
        boolean forceAuthn = Boolean.TRUE.equals(saml2IdpRequestContext.isForceAuthn());
        if (forceAuthn) {
            log.info("ForceAuthn requested but user already authenticated");
        }
        // 判断是否已认证
        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = currentAuthentication != null
                && currentAuthentication.isAuthenticated()
                && !(currentAuthentication instanceof AnonymousAuthenticationToken);
        if (!authenticated) {
            // 保存上下文
            saml2IdpRequestContextRepository.save(spEntityId, saml2IdpRequestContext);
            throw new Saml2IdpAuthenticationRequiredException(spEntityId, requestId);
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
