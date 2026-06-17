package com.atlas.auth.domain.dto;

import java.util.Map;

public record OAuth2ProviderSettings(
        String clientName,
        String clientId,
        String clientSecret,
        String redirectUrl,
        String scope,
        Endpoints endpoints,
        ExtraParams extraParams
) {

    public record Endpoints(
            EndpointConfig token,
            EndpointConfig userInfo,
            EndpointConfig authorizeCode,
            EndpointConfig userEmail,
            EndpointConfig qrScan
    ) {}

    public record EndpointConfig(
            String url,
            String method
    ) {}

    public record ExtraParams(
            Map<String, String> authorize,
            Map<String, String> token
    ) {}

}
