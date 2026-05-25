package com.atlas.auth.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record WebauthnRegistrationCredential(
        @NotBlank
        String id,

        @NotBlank
        String rawId,

        @NotBlank
        String type,

        @Valid
        @NotNull
        RegistrationResponse response,

        String authenticatorAttachment,

        Map<String, Object> clientExtensionResults
) {
}
