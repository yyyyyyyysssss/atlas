package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordLoginDTO(

        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password,

        String clientType
) {
}
