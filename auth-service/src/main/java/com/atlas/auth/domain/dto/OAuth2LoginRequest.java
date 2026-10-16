package com.atlas.auth.domain.dto;

public record OAuth2LoginRequest(
        String code,
        String state,
        String codeVerifier
) implements ThirdPartyLoginRequest {

}
