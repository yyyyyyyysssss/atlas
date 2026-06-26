package com.atlas.auth.domain.dto;

import org.springframework.web.util.UriComponentsBuilder;

public record SsoProviderAuthorizeUrlResponse(
        String url,

        String state,

        boolean pkceRequired
) {

    public static SsoProviderAuthorizeUrlResponse of(String url, boolean pkceRequired){
        String extractedState = UriComponentsBuilder.fromUriString(url)
                .build()
                .getQueryParams()
                .getFirst("state");
        return new SsoProviderAuthorizeUrlResponse(url, extractedState, pkceRequired);
    }

}
