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
) implements SsoSettings, Decryptable<OidcProviderSettings>{

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

    public record ExtraParams(
            Map<String, String> authorize,
            Map<String, String> token
    ) {}
}
