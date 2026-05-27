package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record TotpUnbindDTO(
        @NotBlank(message = "安全验证凭证缺失，请重新进行身份验证")
        String ticket
) {
}
