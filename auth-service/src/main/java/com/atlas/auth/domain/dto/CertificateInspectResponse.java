package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.CertificateIdentityType;

import java.time.Instant;
import java.util.List;

public record CertificateInspectResponse(
        CertificateIdentityType identityType,

        // SHA-256 证书指纹
        String fingerprint,

        // 证书公钥指纹
        String publicKeyFingerprint,

        // 证书序列号
        String serialNumber,

        // 完整 Subject
        String subject,

        // Issuer
        String issuer,

        // 证书生效时间
        Instant notBefore,

        // 证书过期时间
        Instant notAfter,

        // DNS / IP SAN
        List<String> subjectAlternativeNames
) {
}
