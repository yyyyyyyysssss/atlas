package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.SecurityScene;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GestureVerifyDTO(

        @NotBlank(message = "手势不能为空")
        String gesture,

        @NotNull(message = "业务场景不能为空")
        SecurityScene securityScene
) {
}
