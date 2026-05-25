package com.atlas.auth.domain.dto;

public record WebauthnAuthenticateResponse(
        String credentialId,

        String userHandle,

        boolean verified
) {
}
