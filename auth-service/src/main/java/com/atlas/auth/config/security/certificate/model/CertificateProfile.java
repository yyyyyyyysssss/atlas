package com.atlas.auth.config.security.certificate.model;

public record CertificateProfile(
        String id,
        String keyAlgorithm,
        int keySize,
        String ecCurve,
        int validityDays
) {
}
