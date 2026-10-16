package com.atlas.auth.domain.dto;

import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;

public record Saml2LoginRequest(
        Saml2AuthenticatedPrincipal principal
) implements ThirdPartyLoginRequest {

}
