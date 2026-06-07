package com.atlas.auth.domain.dto;

import com.atlas.security.model.MfaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MfaLoginDTO(

        @NotBlank(message = "凭证不能为空")
        String ticket,

        @NotBlank(message = "验证码不能为空")
        String code,

        @NotNull(message = "验证类型不能为空")
        MfaType mfaType
) {
}
