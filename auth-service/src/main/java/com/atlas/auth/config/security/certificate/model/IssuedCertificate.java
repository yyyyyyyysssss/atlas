package com.atlas.auth.config.security.certificate.model;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;

public record IssuedCertificate(
        X509Certificate certificate,
        PrivateKey privateKey,
        List<X509Certificate> certificateChain,
        String serialNumber,
        String fingerprint,
        Instant notBefore,
        Instant notAfter
) {

    public IssuedCertificate {
        certificateChain = List.copyOf(certificateChain);
    }

}
