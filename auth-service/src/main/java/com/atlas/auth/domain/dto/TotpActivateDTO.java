package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TotpActivateDTO(
        @NotNull(message = "验证码不能为空")
        @Min(value = 100000, message = "验证码必须为 6 位数字")
        @Max(value = 999999, message = "验证码必须为 6 位数字")
        Integer code
) {
}
