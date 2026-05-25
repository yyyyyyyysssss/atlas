package com.atlas.auth.domain.dto;

public record WebauthnRegistrationResponse(
        String credentialId,
        boolean isSuccess
) {
}
