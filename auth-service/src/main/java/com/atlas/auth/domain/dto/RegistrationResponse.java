package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record RegistrationResponse(

        @NotBlank
        String clientDataJSON,

        @NotBlank
        String attestationObject,

        String authenticatorData,

        String publicKey,

        Integer publicKeyAlgorithm,

        List<String> transports
) {
}
