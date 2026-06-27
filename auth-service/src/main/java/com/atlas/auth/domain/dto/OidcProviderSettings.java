package com.atlas.auth.domain.dto;

import com.atlas.security.utils.AesUtils;
import org.springframework.security.oauth2.core.oidc.OidcScopes;

import java.util.Map;

public record OidcProviderSettings(
        String clientName,
        String clientId,
        String clientSecret,
        String redirectUrl,
        String scope,
        String issuerUrl,
        ExtraParams extraParams
) implements SsoSettings, Decryptable<OidcProviderSettings>, BaseUrlAppliable<OidcProviderSettings>{

    private static final String BASE_URL_PLACEHOLDER = "{{baseUrl}}";

    public OidcProviderSettings {
        if (scope == null || scope.isBlank()) {
            scope = String.join(" ",
                    OidcScopes.OPENID,
                    OidcScopes.PROFILE,
                    OidcScopes.EMAIL
            );
        }
    }

    @Override
    public OidcProviderSettings decrypt(String key) {
        String decryptedSecret = AesUtils.decrypt(this.clientSecret, key);
        return new OidcProviderSettings(
                this.clientName,
                this.clientId,
                decryptedSecret,
                this.redirectUrl,
                this.scope,
                this.issuerUrl,
                this.extraParams
        );
    }

    @Override
    public OidcProviderSettings applyBaseUrl(String baseUrl) {
        if (this.issuerUrl == null || this.issuerUrl.isBlank() || !this.issuerUrl.contains(BASE_URL_PLACEHOLDER)) {
            return this;
        }
        return new OidcProviderSettings(
                this.clientName,
                this.clientId,
                this.clientSecret,
                this.redirectUrl,
                this.scope,
                baseUrl,
                this.extraParams
        );
    }

    public record ExtraParams(
            Map<String, String> authorize,
            Map<String, String> token
    ) {}
}
