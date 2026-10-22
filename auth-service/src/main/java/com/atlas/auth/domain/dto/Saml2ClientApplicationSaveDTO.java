package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record Saml2ClientApplicationSaveDTO(

        Long id,

        @NotBlank(message = "应用名称不能为空")
        String applicationName,

        @NotBlank(message = "Entity ID不能为空")
        String entityId,

        @NotBlank(message = "回调地址不能为空")
        String acsUrl,

        Boolean requireSignedAuthnRequest,

        String verificationCertificate,

        Boolean signResponse,

        Boolean signAssertion,

        Map<String, String> attributeMappings,

        String description
) {
}
