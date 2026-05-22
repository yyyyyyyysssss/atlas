package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyPasswordDTO(
        @NotBlank(message = "密码不能为空")
        String password
) {
}
