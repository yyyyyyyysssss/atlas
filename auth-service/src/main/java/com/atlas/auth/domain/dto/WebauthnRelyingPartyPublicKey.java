package com.atlas.auth.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.web.webauthn.api.*;
import org.springframework.security.web.webauthn.management.RelyingPartyPublicKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/25 15:55
 */
public record WebauthnRelyingPartyPublicKey (

        @NotBlank
        String label,

        @Valid
        @NotNull
        WebauthnRegistrationCredential credential

){
    public RelyingPartyPublicKey toRelyingPartyPublicKey() {
        RegistrationResponse response = credential.response();
        AuthenticatorAttestationResponse attestationResponse = AuthenticatorAttestationResponse.builder()
                .clientDataJSON(
                        Bytes.fromBase64(
                                response.clientDataJSON()
                        )
                )
                .attestationObject(
                        Bytes.fromBase64(
                                response.attestationObject()
                        )
                )
                .transports(
                        toAuthenticatorTransports(
                                response.transports()
                        )
                )
                .build();

        PublicKeyCredential<AuthenticatorAttestationResponse> publicKeyCredential = PublicKeyCredential.<AuthenticatorAttestationResponse>builder()
                .id(credential.id())
                .rawId(
                        Bytes.fromBase64(
                                credential.rawId()
                        )
                )
                .response(attestationResponse)
                .authenticatorAttachment(
                        toAuthenticatorAttachment(
                                credential.authenticatorAttachment()
                        )
                )
                .clientExtensionResults(
                        toClientExtensionOutputs(credential.clientExtensionResults())
                )
                .type(
                        PublicKeyCredentialType.PUBLIC_KEY
                )
                .build();

        return new RelyingPartyPublicKey(
                publicKeyCredential,
                sanitizeLabel(label)
        );
    }

    private static List<AuthenticatorTransport> toAuthenticatorTransports(List<String> transports) {

        if (transports == null || transports.isEmpty()) {
            return List.of();
        }

        return transports.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .map(AuthenticatorTransport::valueOf)
                .toList();
    }

    private static AuthenticatorAttachment toAuthenticatorAttachment(String attachment) {
        if (attachment == null || attachment.isBlank()) {
            return null;
        }

        return AuthenticatorAttachment.valueOf(attachment.trim().toLowerCase());
    }

    private static AuthenticationExtensionsClientOutputs toClientExtensionOutputs(Map<String, Object> extensions) {

        if (extensions == null || extensions.isEmpty()) {
            return new ImmutableAuthenticationExtensionsClientOutputs();
        }

        List<AuthenticationExtensionsClientOutput<?>> outputs =
                new ArrayList<>();

        Object credProps = extensions.get("credProps");

        if (credProps instanceof Map<?, ?> credPropsMap) {

            Object rk = credPropsMap.get("rk");

            if (rk instanceof Boolean rkValue) {

                outputs.add(
                        new CredentialPropertiesOutput(rkValue)
                );
            }
        }

        return new ImmutableAuthenticationExtensionsClientOutputs(
                outputs
        );
    }

    private static String sanitizeLabel(String label) {

        if (label == null) {
            return "Unnamed Passkey";
        }

        String sanitized = label.trim();

        if (sanitized.length() > 128) {
            sanitized = sanitized.substring(0, 128);
        }

        return sanitized;
    }

}
