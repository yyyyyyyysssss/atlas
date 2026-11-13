package com.atlas.auth.config.security.certificate;


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "security.certificate")
public record CertificateProperties(
        boolean enabled,
        String keyStore,
        String keyStorePassword,
        String keyAlias,
        Map<String, Profile> profiles
) {

    public record Profile(
            String keyAlgorithm,
            int keySize,
            String ecCurve,
            int validityDays
    ) {
    }

}
