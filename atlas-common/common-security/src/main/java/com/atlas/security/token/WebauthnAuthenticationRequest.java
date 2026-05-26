package com.atlas.security.token;

import org.springframework.security.web.webauthn.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record WebauthnAuthenticationRequest(
        String id,

        String type,

        String rawId,

        AssertionResponse response,

        String authenticatorAttachment,

        Map<String, Object> clientExtensionResults
) {

    public static PublicKeyCredential<AuthenticatorAssertionResponse> toCredential(WebauthnAuthenticationRequest dto) {

        // 1. convert response
        AuthenticatorAssertionResponse response =
                AuthenticatorAssertionResponse.builder()
                        .clientDataJSON(Bytes.fromBase64(
                                dto.response().clientDataJSON()
                        ))
                        .authenticatorData(Bytes.fromBase64(
                                dto.response().authenticatorData()
                        ))
                        .signature(Bytes.fromBase64(
                                dto.response().signature()
                        ))
                        .userHandle(dto.response().userHandle() != null
                                ? Bytes.fromBase64(dto.response().userHandle())
                                : null
                        )
                        .build();

        // 2. convert credential
        return PublicKeyCredential.<AuthenticatorAssertionResponse>builder()
                .id(dto.id())
                .type(PublicKeyCredentialType.PUBLIC_KEY)
                .rawId(Bytes.fromBase64(dto.rawId()))
                .response(response)
                .authenticatorAttachment(
                        dto.authenticatorAttachment() != null
                                ? AuthenticatorAttachment.valueOf(
                                dto.authenticatorAttachment().trim().toLowerCase()
                        )
                                : null
                )
                .clientExtensionResults(
                        toClientExtensionOutputs(dto.clientExtensionResults)
                )
                .build();
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

    public record AssertionResponse(

            String clientDataJSON,

            String authenticatorData,

            String signature,

            String userHandle

    ) {
    }

}
