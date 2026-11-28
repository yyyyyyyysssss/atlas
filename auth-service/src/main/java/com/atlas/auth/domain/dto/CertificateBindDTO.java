package com.atlas.auth.domain.dto;

public record CertificateBindDTO(

        String label,

        String csr,

        String ticket


) {
}
