package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotNull;

public record ApplicationEnabledDTO(
        @NotNull(message = "启用状态不能为空")
        Boolean enabled
) {
}
