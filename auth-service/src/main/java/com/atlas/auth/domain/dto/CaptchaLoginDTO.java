package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CaptchaLoginDTO(

        @NotBlank(message = "认证标识不能为空")
        String identity,

        @NotBlank(message = "验证码不能为空")
        String captcha,

        @NotNull(message = "认证类型不能为空")
        Type type,

        String clientType
) {

    public enum Type {
        PHONE,
        EMAIL
    }

}
