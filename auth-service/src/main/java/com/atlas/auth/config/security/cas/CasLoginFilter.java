package com.atlas.auth.config.security.cas;

import com.atlas.auth.config.exception.AuthErrorCode;
import com.atlas.common.security.properties.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/29 15:49
 */
@Slf4j
public class CasLoginFilter extends OncePerRequestFilter {

    private static final String DEFAULT_LOGIN_ENDPOINT_URI = "/cas/login";

    private final RequestMatcher requestMatcher;

    private final AuthenticationManager authenticationManager;

    private AuthenticationSuccessHandler authenticationSuccessHandler;

    private AuthenticationFailureHandler authenticationFailureHandler;

    private AuthenticationEntryPoint authenticationEntryPoint;

    private final AuthenticationConverter authenticationConverter;

    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource;

    public CasLoginFilter(AuthenticationManager authenticationManager, SecurityProperties securityProperties) {
        this.requestMatcher = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, DEFAULT_LOGIN_ENDPOINT_URI);
        this.authenticationManager = authenticationManager;
        this.authenticationDetailsSource = new WebAuthenticationDetailsSource();
        this.authenticationEntryPoint = new CasAuthenticationEntryPoint(securityProperties);
        this.authenticationConverter = new DelegatingAuthenticationConverter(Arrays.asList(new CasRequestContextAuthenticationConverter()));
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
        } catch (CasAuthenticationRequiredException e) {
            log.debug("Cas authentication required", e);
            this.authenticationEntryPoint.commence(request, response, e);
        } catch (CasAuthenticationException e) {
            log.warn("cas authentication failed: {}", e.getMessage());
            // CAS协议异常
            if (this.authenticationFailureHandler != null) {
                authenticationFailureHandler.onAuthenticationFailure(request, response, e);
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (Exception e) {
            // 内部未知异常
            log.error("Unexpected Cas error", e);
            CasAuthenticationException exception = new CasAuthenticationException(AuthErrorCode.CAS_INTERNAL_ERROR, "CAS internal error", e);
            if (this.authenticationFailureHandler != null) {
                authenticationFailureHandler.onAuthenticationFailure(request, response, exception);
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    public void setAuthenticationEntryPoint(AuthenticationEntryPoint authenticationEntryPoint) {
        Objects.requireNonNull(authenticationEntryPoint);
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    public void setAuthenticationSuccessHandler(AuthenticationSuccessHandler authenticationSuccessHandler) {
        Objects.requireNonNull(authenticationSuccessHandler);
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    public void setAuthenticationFailureHandler(AuthenticationFailureHandler authenticationFailureHandler) {
        Objects.requireNonNull(authenticationFailureHandler);
        this.authenticationFailureHandler = authenticationFailureHandler;
    }

    public void setAuthenticationDetailsSource(AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
        Assert.notNull(authenticationDetailsSource, "authenticationDetailsSource cannot be null");
        this.authenticationDetailsSource = authenticationDetailsSource;
    }

    public static CasAuthenticationException invalidRequest(String description) {

        return new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, description);
    }
}
