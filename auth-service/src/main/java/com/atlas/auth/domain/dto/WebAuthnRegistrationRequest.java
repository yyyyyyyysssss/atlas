package com.atlas.auth.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record WebAuthnRegistrationRequest(

        @Valid
        @NotNull
        WebauthnRelyingPartyPublicKey publicKey,

        String ticket
) {


}
