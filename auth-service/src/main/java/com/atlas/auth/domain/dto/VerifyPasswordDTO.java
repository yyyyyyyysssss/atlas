package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.SecurityScene;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerifyPasswordDTO(
        @NotBlank(message = "密码不能为空")
        String password,

        @NotNull(message = "业务场景不能为空")
        SecurityScene securityScene

) {
}
