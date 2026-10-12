package com.atlas.auth.config.security.saml2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.util.Locale;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/12 10:37
 */
public class Saml2IdpRequestContextAuthenticationConverter implements AuthenticationConverter {

    // 在 Base64 解码之前就拦截超长输入，减少不必要的内存分配
    private static final int MAX_SAML_REQUEST_LENGTH = 1_400_000;

    private static final int MAX_RELAY_STATE_LENGTH = 80;

    @Override
    public Authentication convert(HttpServletRequest request) {
        String samlRequest = getSingleParameter(request, "SAMLRequest");
        // 不是SAML请求，让其它Converter处理
        if (samlRequest == null || samlRequest.isBlank()) {
            return null;
        }
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
        String relayState = getSingleParameter(request, "RelayState");
        String rawQueryString = request.getQueryString();
        if (samlRequest.length() > MAX_SAML_REQUEST_LENGTH) {
            throw invalidRequest("SAMLRequest exceeds the maximum allowed length");
        }
        if (relayState != null && relayState.length() > MAX_RELAY_STATE_LENGTH) {
            throw invalidRequest("RelayState exceeds the maximum allowed length");
        }
        return new Saml2IdpRequestAuthenticationToken(samlRequest, binding.name(), relayState, rawQueryString);
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

    public static Saml2IdpAuthenticationException invalidRequest(String description) {

        return new Saml2IdpAuthenticationException(new Saml2Error(Saml2ErrorCodes.INVALID_REQUEST, description));
    }
}
