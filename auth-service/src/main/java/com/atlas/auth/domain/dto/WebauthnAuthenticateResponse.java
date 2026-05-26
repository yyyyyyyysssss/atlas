package com.atlas.auth.domain.dto;

public record WebauthnAuthenticateResponse(
        String credentialId,

        Long userId,

        boolean verified
) {
}
