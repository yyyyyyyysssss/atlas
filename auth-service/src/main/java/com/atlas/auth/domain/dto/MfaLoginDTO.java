package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaLoginDTO(

        @NotBlank(message = "凭证不能为空")
        String ticket,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
        String code
) {
}
