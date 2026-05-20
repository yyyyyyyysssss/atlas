package com.atlas.auth.domain.dto;

import com.atlas.security.enums.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RefreshTokenDTO(
        @NotBlank(message = "token不能为空")
        String token,

        @NotNull(message = "客户端类型不能为空")
        ClientType clientType
) {
}
