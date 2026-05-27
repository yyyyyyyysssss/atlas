package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.SecurityScene;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TotpVerifyDTO(

        @NotNull(message = "验证码不能为空")
        @Min(value = 100000, message = "验证码必须为 6 位数字")
        @Max(value = 999999, message = "验证码必须为 6 位数字")
        Integer code,

        @NotNull(message = "业务场景不能为空")
        SecurityScene securityScene
) {
}
