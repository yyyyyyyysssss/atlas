package com.atlas.auth.domain.dto;

import com.atlas.common.security.enums.ClientType;
import com.atlas.common.security.token.WebauthnAuthenticationRequest;
import jakarta.validation.constraints.NotNull;

public record WebauthnLoginDTO(
        @NotNull(message = "客户端类型不能为空")
        ClientType clientType,

        @NotNull(message = "通行密钥凭证不能为空")
        WebauthnAuthenticationRequest webauthnAuthenticationRequest
) {
}
