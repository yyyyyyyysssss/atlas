package com.atlas.auth.domain.dto;

public record CertificateAuthenticateResponse(

        Long userId,

        String fingerprint

) {
}
