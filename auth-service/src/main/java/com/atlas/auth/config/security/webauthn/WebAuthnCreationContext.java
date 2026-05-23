package com.atlas.auth.config.security.webauthn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebAuthnCreationContext implements Serializable {

    private String challenge;

    private String userId;

    private String userName;

    private String userDisplayName;

    private Long timeoutValue;

    private String attestation;

    private String residentKey;

    private String userVerification;

}
