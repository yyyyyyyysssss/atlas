package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.SecurityScene;
import jakarta.validation.constraints.NotNull;

public record MfaBackupCodeVerifyDTO(

        @NotNull(message = "验证码不能为空")
        String code,

        @NotNull(message = "业务场景不能为空")
        SecurityScene securityScene
) {
}
