package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.SecurityScene;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record WebauthnAuthenticationDTO(
        String id,
        String rawId,
        String type,
        AssertionResponse response,
        String authenticatorAttachment,

        @NotNull(message = "业务场景不能为空")
        SecurityScene securityScene,

        Map<String, Object> clientExtensionResults // 预留扩展字段接收
) {

    public record AssertionResponse(
            String clientDataJSON,
            String authenticatorData,
            String signature,
            String userHandle
    ) {}

}
