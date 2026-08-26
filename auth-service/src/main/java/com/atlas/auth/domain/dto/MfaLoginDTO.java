package com.atlas.auth.domain.dto;

import com.atlas.security.model.MfaType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MfaLoginDTO(

        @NotBlank(message = "ticket不能为空")
        String ticket,

        @NotNull(message = "验证类型不能为空")
        MfaType mfaType,

        @NotNull(message = "认证凭证不能为空")
        JsonNode credential
) {
}
