package com.atlas.auth.domain.dto;

public record OAuth2ProviderToken(
        String accessToken,
        String tokenType
) {

    public String toAuthorizationHeader() {
        String type = (tokenType == null || tokenType.isBlank()) ? "Bearer" : tokenType;
        return type + " " + accessToken;
    }

}
