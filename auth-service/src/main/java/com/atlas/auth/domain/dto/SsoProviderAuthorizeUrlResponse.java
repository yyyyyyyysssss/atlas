package com.atlas.auth.domain.dto;

public record SsoProviderAuthorizeUrlResponse(
        String url,

        boolean pkceRequired
) {
}
