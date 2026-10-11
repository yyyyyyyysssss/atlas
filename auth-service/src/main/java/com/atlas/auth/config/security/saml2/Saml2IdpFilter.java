package com.atlas.auth.config.security.saml2;

import com.atlas.common.security.token.Saml2IdpAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/10 17:05
 */
@Slf4j
public final class Saml2IdpFilter extends OncePerRequestFilter {

    private static final String DEFAULT_TOKEN_ENDPOINT_URI = "/saml2/idp/sso";

    private final RequestMatcher requestMatcher;

    private final AuthenticationManager authenticationManager;

    private final Saml2IdpService saml2IdpService;

    private AuthenticationConverter authenticationConverter;

    private AuthenticationSuccessHandler authenticationSuccessHandler;

    private AuthenticationFailureHandler authenticationFailureHandler;

    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource;

    public Saml2IdpFilter(AuthenticationManager authenticationManager, Saml2IdpService saml2IdpService) {
        this.requestMatcher = PathPatternRequestMatcher.withDefaults().matcher(DEFAULT_TOKEN_ENDPOINT_URI);
        this.authenticationManager = authenticationManager;
        this.saml2IdpService = saml2IdpService;
        this.authenticationDetailsSource = new WebAuthenticationDetailsSource();
        this.authenticationConverter = new DelegatingAuthenticationConverter(Arrays.asList(new Saml2IdpAuthenticationConverter()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!this.requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            Authentication authorizationGrantAuthentication = this.authenticationConverter.convert(request);
            if (authorizationGrantAuthentication == null) {
                throw invalidRequest("Unsupported SAML authentication request");
            }
            if (authorizationGrantAuthentication instanceof AbstractAuthenticationToken authenticationToken) {
                authenticationToken.setDetails(this.authenticationDetailsSource.buildDetails(request));
            }
            Saml2IdpAuthenticationToken saml2IdpAuthenticationToken = (Saml2IdpAuthenticationToken) this.authenticationManager.authenticate(authorizationGrantAuthentication);
            if (this.authenticationSuccessHandler != null) {
                this.authenticationSuccessHandler.onAuthenticationSuccess(request, response, saml2IdpAuthenticationToken);
            }
        } catch (Saml2IdpAuthenticationException e){
            log.warn("SAML authentication failed: {}", e.getMessage());
            // SAML协议异常
            writeSamlErrorResponse(response, e);
        } catch (AuthenticationException e) {
            // 自身认证异常
            if(authenticationFailureHandler != null){
                this.authenticationFailureHandler.onAuthenticationFailure(request, response, e);
            } else {
                throw e;
            }
        } catch (Exception e) {
            // IdP内部未知异常
            log.error("Unexpected SAML IdP error", e);
            writeSamlErrorResponse(response, new Saml2IdpAuthenticationException(new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "Internal SAML IdP error"), null));
        }
    }

    public void setAuthenticationSuccessHandler(AuthenticationSuccessHandler authenticationSuccessHandler) {
        Assert.notNull(authenticationSuccessHandler, "authenticationSuccessHandler cannot be null");
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    public void setAuthenticationFailureHandler(AuthenticationFailureHandler authenticationFailureHandler) {
        Assert.notNull(authenticationFailureHandler, "authenticationFailureHandler cannot be null");
        this.authenticationFailureHandler = authenticationFailureHandler;
    }

    public void setAuthenticationDetailsSource(AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
        Assert.notNull(authenticationDetailsSource, "authenticationDetailsSource cannot be null");
        this.authenticationDetailsSource = authenticationDetailsSource;
    }

    private void writeSamlErrorResponse(HttpServletResponse response, Saml2IdpAuthenticationException e) throws IOException {
        String responseXml = saml2IdpService.buildErrorResponse(e);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/saml+xml");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(responseXml);
        response.getWriter().flush();
    }

    public static Saml2IdpAuthenticationException invalidRequest(String description) {

        return new Saml2IdpAuthenticationException(new Saml2Error(Saml2ErrorCodes.INVALID_REQUEST, description));
    }
}
