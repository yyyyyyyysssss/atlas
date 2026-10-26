package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record Saml2ClientSaveDTO(

        Long id,

        @NotBlank(message = "客户端名称不能为空")
        String clientName,

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
