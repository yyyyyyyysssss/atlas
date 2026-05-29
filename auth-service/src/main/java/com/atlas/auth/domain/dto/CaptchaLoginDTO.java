package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.CaptchaType;
import com.atlas.security.enums.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CaptchaLoginDTO(

        @NotBlank(message = "认证标识不能为空")
        String identity,

        @NotBlank(message = "验证码不能为空")
        String captcha,

        @NotNull(message = "验证码类型不能为空")
        CaptchaType captchaType,

        @NotNull(message = "客户端类型不能为空")
        ClientType clientType
) {

}
