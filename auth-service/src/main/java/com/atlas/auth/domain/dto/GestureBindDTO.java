package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record GestureBindDTO(

        @NotBlank(message = "安全验证凭证缺失，请重新进行身份验证")
        String ticket,

        @NotBlank(message = "手势不能为空")
        String gesture,

        @NotBlank(message = "确认手势不能为空")
        String confirmGesture
) {
}
