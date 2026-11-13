package com.atlas.auth.config.security.certificate.model;

public record CertificateSubject(
        String commonName,
        String organization,
        String organizationalUnit
) {
}
