package com.atlas.auth.domain.dto;

public record OAuth2ProviderAuthorizeUrlResponse(
        String url,

        boolean pkceRequired
) {
}
