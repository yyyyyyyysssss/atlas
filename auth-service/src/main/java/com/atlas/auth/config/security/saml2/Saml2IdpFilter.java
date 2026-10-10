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
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

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

    private AuthenticationConverter authenticationConverter;

    private AuthenticationSuccessHandler authenticationSuccessHandler;

    private AuthenticationFailureHandler authenticationFailureHandler;

    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource;

    // 防止超大 XML 被解析1 最大限制1MB
    private static final int MAX_SAML_XML_BYTES = 1024 * 1024;// 1 MiB

    // 限制HTTP-Redirect 解码后的压缩数据的大小，避免处理异常大的请求
    private static final int MAX_REDIRECT_REQUEST_BYTES = 256 * 1024; // 256 KiB

    // 在 Base64 解码之前就拦截超长输入，减少不必要的内存分配
    private static final int MAX_SAML_REQUEST_LENGTH = 1_400_000;

    private static final int MAX_RELAY_STATE_LENGTH = 80;

    public Saml2IdpFilter(AuthenticationManager authenticationManager) {
        this.requestMatcher = PathPatternRequestMatcher.withDefaults().matcher(DEFAULT_TOKEN_ENDPOINT_URI);
        this.authenticationManager = authenticationManager;
        this.authenticationDetailsSource = new WebAuthenticationDetailsSource();
        this.authenticationConverter = new DelegatingAuthenticationConverter(Arrays.asList(new Saml2IdpRedirectAuthenticationConverter(), new Saml2IdpPostAuthenticationConverter()));
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
        } catch (AuthenticationException e) {
            if (this.authenticationFailureHandler != null) {
                this.authenticationFailureHandler.onAuthenticationFailure(request, response, e);
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (Exception e) {
            log.error("SAML IdP request processing failed", e);
            if (this.authenticationFailureHandler != null) {
                this.authenticationFailureHandler.onAuthenticationFailure(request, response, invalidRequest("SAML AuthnRequest processing failed"));
            }
        }
    }

    private Saml2IdpRequest resolveRequest(HttpServletRequest request) {
        Saml2MessageBinding binding;
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            binding = Saml2MessageBinding.REDIRECT;
        } else if ("POST".equalsIgnoreCase(request.getMethod())) {
            binding = Saml2MessageBinding.POST;
        } else {
            throw invalidRequest("Unsupported HTTP method");
        }
        if (binding == Saml2MessageBinding.POST) {
            String contentType = request.getContentType();
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("application/x-www-form-urlencoded")) {
                throw invalidRequest("Unsupported SAML POST content type");
            }
        }
        String samlRequest = getSingleParameter(request, "SAMLRequest");
        String relayState = getSingleParameter(request, "RelayState");
        String rawQueryString = request.getQueryString();
        if (samlRequest == null || samlRequest.isBlank()) {
            throw invalidRequest("SAMLRequest parameter is required");
        }
        if (samlRequest.length() > MAX_SAML_REQUEST_LENGTH) {
            throw invalidRequest("SAMLRequest exceeds the maximum allowed length");
        }
        if (relayState != null && relayState.length() > MAX_RELAY_STATE_LENGTH) {
            throw invalidRequest("RelayState exceeds the maximum allowed length");
        }
        return new Saml2IdpRequest(binding, samlRequest, relayState, rawQueryString);
    }

    public void setAuthenticationSuccessHandler(AuthenticationSuccessHandler authenticationSuccessHandler) {
        Assert.notNull(authenticationSuccessHandler, "authenticationSuccessHandler cannot be null");
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    public void setAuthenticationFailureHandler(AuthenticationFailureHandler authenticationFailureHandler) {
        Assert.notNull(authenticationFailureHandler, "authenticationFailureHandler cannot be null");
        this.authenticationFailureHandler = authenticationFailureHandler;
    }

    private String getSingleParameter(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null || values.length == 0) {
            return null;
        }
        if (values.length != 1) {
            throw invalidRequest("Duplicate " + name + " parameter");
        }

        return values[0];
    }

    public static Saml2AuthenticationException invalidRequest(String description) {

        return new Saml2AuthenticationException(new Saml2Error("urn:oasis:names:tc:SAML:2.0:status:Requester", description));
    }
}
