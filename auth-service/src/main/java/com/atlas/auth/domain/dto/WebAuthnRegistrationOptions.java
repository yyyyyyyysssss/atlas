package com.atlas.auth.domain.dto;

import org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientInput;
import org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientInputs;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record WebAuthnRegistrationOptions(

        Rp rp,

        User user,

        String challenge,

        List<PubKeyCredParam> pubKeyCredParams,

        Long timeout,

        List<CredentialDescriptor> excludeCredentials,

        AuthenticatorSelection authenticatorSelection,

        String attestation,

        Map<String, Object> extensions

) {

    public static WebAuthnRegistrationOptions of(PublicKeyCredentialCreationOptions source) {

        return new WebAuthnRegistrationOptions(

                new Rp(
                        source.getRp().getName(),
                        source.getRp().getId()
                ),

                new User(
                        source.getUser().getName(),
                        source.getUser().getId().toBase64UrlString(),
                        source.getUser().getDisplayName()
                ),

                source.getChallenge().toBase64UrlString(),

                source.getPubKeyCredParams()
                        .stream()
                        .map(item -> new PubKeyCredParam(
                                item.getType().getValue(),
                                item.getAlg().getValue()
                        ))
                        .toList(),

                source.getTimeout().toMillis(),

                source.getExcludeCredentials()
                        .stream()
                        .map(item -> new CredentialDescriptor(
                                item.getType().getValue(),
                                item.getId().toBase64UrlString()
                        ))
                        .toList(),

                source.getAuthenticatorSelection() == null
                        ? null
                        : new AuthenticatorSelection(
                        source.getAuthenticatorSelection().getResidentKey().getValue(),
                        source.getAuthenticatorSelection().getUserVerification().getValue(),
                        source.getAuthenticatorSelection().getAuthenticatorAttachment() == null
                                ? null : source.getAuthenticatorSelection().getAuthenticatorAttachment().getValue() // 👈
                ),

                source.getAttestation() == null
                        ? null
                        : source.getAttestation().getValue(),

                convertExtensions(source.getExtensions())
        );
    }

    private static Map<String, Object> convertExtensions(AuthenticationExtensionsClientInputs extensions) {

        if (extensions == null || extensions.getInputs().isEmpty()) {
            return Map.of();
        }

        Map<String, Object> result = new LinkedHashMap<>();

        for (AuthenticationExtensionsClientInput input : extensions.getInputs()) {
            result.put(
                    input.getExtensionId(),
                    input.getInput()
            );
        }

        return result;
    }

    public record Rp(
            String name,
            String id
    ) {
    }

    public record User(
            String name,
            String id,
            String displayName
    ) {
    }

    public record PubKeyCredParam(
            String type,
            Long alg
    ) {
    }

    public record CredentialDescriptor(
            String type,
            String id
    ) {
    }

    public record AuthenticatorSelection(
            String residentKey,
            String userVerification,
            String authenticatorAttachment
    ) {
    }

}
