package com.atlas.auth.domain.dto;

import com.atlas.security.model.MfaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MfaLoginDTO(

        @NotBlank(message = "凭证不能为空")
        String ticket,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^(\\d{6}|[a-zA-Z0-9]{5}-[a-zA-Z0-9]{5})$", message = "验证码格式不正确")
        String code,

        @NotNull(message = "验证类型不能为空")
        MfaType mfaType
) {
}
