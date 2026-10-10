package com.atlas.auth.config.security.saml2;

import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

public record Saml2IdpRequest(
        Saml2MessageBinding binding,
        String samlRequest,
        String relayState,
        String rawQueryString
) {
}
