package com.atlas.auth.domain.dto;

import org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientInput;
import org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientInputs;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record WebauthnAuthenticateOptions(
        String challenge,

        Long timeout,

        String rpId,

        List<AllowCredential> allowCredentials,

        String userVerification,

        Map<String, Object> extensions
) {

    public static WebauthnAuthenticateOptions of(PublicKeyCredentialRequestOptions source) {

        return new WebauthnAuthenticateOptions(
                source.getChallenge().toBase64UrlString(),
                source.getTimeout() != null ? source.getTimeout().toMillis() : null,
                source.getRpId(),

                source.getAllowCredentials()
                        .stream()
                        .map(it -> new AllowCredential(
                                it.getType().getValue(),
                                it.getId().toBase64UrlString(),
                                it.getTransports() == null
                                        ? List.of()
                                        : it.getTransports()
                                        .stream()
                                        .map(AuthenticatorTransport::getValue)
                                        .toList()
                        ))
                        .toList(),

                source.getUserVerification() != null
                        ? source.getUserVerification().getValue()
                        : null,

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

    public record AllowCredential(

            String type,

            String id,

            List<String> transports

    ) {
    }

}
