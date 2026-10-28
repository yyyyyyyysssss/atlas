package com.atlas.auth.config.security.saml2;

import com.atlas.common.security.properties.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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

    private final AuthenticationConverter authenticationConverter;

    private AuthenticationSuccessHandler authenticationSuccessHandler;

    private AuthenticationFailureHandler authenticationFailureHandler;

    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource;

    private AuthenticationEntryPoint authenticationEntryPoint;

    public Saml2IdpFilter(AuthenticationManager authenticationManager,
                          Saml2IdpService saml2IdpService,
                          SecurityProperties securityProperties,
                          Saml2IdpRequestContextRepository repository,
                          ApplicationEventPublisher eventPublisher) {
        this.requestMatcher = PathPatternRequestMatcher.withDefaults().matcher(DEFAULT_TOKEN_ENDPOINT_URI);
        this.authenticationManager = authenticationManager;
        this.authenticationDetailsSource = new WebAuthenticationDetailsSource();
        this.authenticationConverter = new DelegatingAuthenticationConverter(Arrays.asList(new Saml2IdpRequestContextAuthenticationConverter(), new Saml2IdpAuthenticationConverter(repository)));
        this.authenticationEntryPoint = new Saml2IdpAuthenticationEntryPoint(securityProperties);
        this.authenticationSuccessHandler = new Saml2IdpAuthenticationSuccessHandler(saml2IdpService, eventPublisher);
        this.authenticationFailureHandler = new Saml2IdpAuthenticationFailureHandler(saml2IdpService);
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
            Authentication authenticated = this.authenticationManager.authenticate(authorizationGrantAuthentication);
            this.authenticationSuccessHandler.onAuthenticationSuccess(request, response, authenticated);
        } catch (Saml2IdpAuthenticationException e){
            log.warn("SAML authentication failed: {}", e.getMessage());
            // SAML协议异常
            authenticationFailureHandler.onAuthenticationFailure(request, response, e);
        } catch (Saml2IdpAuthenticationRequiredException e){
            log.debug("SAML authentication required, requestId={}, issuer={}", e.getRequestId(), e.getIssuer());
            this.authenticationEntryPoint.commence(request, response, e);
        }  catch (Exception e) {
            // IdP内部未知异常
            log.error("Unexpected SAML IdP error", e);
            authenticationFailureHandler.onAuthenticationFailure(request, response, new Saml2IdpAuthenticationException(new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "Internal SAML IdP error"), null));
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

    public void setAuthenticationEntryPoint(AuthenticationEntryPoint authenticationEntryPoint) {
        Assert.notNull(authenticationEntryPoint, "authenticationEntryPoint cannot be null");
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    public void setAuthenticationDetailsSource(AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
        Assert.notNull(authenticationDetailsSource, "authenticationDetailsSource cannot be null");
        this.authenticationDetailsSource = authenticationDetailsSource;
    }

    public static Saml2IdpAuthenticationException invalidRequest(String description) {

        return new Saml2IdpAuthenticationException(new Saml2Error(Saml2ErrorCodes.INVALID_REQUEST, description));
    }
}
