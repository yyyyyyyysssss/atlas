package com.atlas.auth.domain.dto;

public record WebauthnAuthenticateOptionsResponse(
        String webauthnId,

        WebauthnAuthenticateOptions publicKey
) {

}
