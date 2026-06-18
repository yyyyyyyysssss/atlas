package com.atlas.auth.domain.dto;

import com.atlas.security.utils.AesUtils;

import java.util.Map;

public record OAuth2ProviderSettings(
        String clientName,
        String clientId,
        String clientSecret,
        String redirectUrl,
        String scope,
        Endpoints endpoints,
        boolean pkceRequired,
        ExtraParams extraParams
) implements SsoSettings, Decryptable<OAuth2ProviderSettings> {

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

    @Override
    public OAuth2ProviderSettings decrypt(String key) {
        String decryptedSecret = AesUtils.decrypt(this.clientSecret, key);
        return new OAuth2ProviderSettings(
                this.clientName,
                this.clientId,
                decryptedSecret,
                this.redirectUrl,
                this.scope,
                this.endpoints,
                this.pkceRequired,
                this.extraParams
        );
    }
}
