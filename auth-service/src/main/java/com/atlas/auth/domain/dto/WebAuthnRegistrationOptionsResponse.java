package com.atlas.auth.domain.dto;

public record WebAuthnRegistrationOptionsResponse(
        String webauthnId,

        WebAuthnRegistrationOptions publicKey
) {
}
