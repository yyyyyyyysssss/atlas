package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.CaptchaScene;
import com.atlas.auth.enums.CaptchaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CaptchaSendDTO(

        @NotBlank(message = "接收目标不能为空")
        String target,

        @NotNull(message = "验证码类型不能为空")
        CaptchaType captchaType,

        @NotNull(message = "业务场景不能为空")
        CaptchaScene captchaScene
) {

}
