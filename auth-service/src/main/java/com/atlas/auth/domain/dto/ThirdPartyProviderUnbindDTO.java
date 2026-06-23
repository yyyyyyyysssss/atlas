package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ThirdPartyProviderUnbindDTO(

        @NotBlank(message = "安全验证凭证缺失，请重新进行身份验证")
        String ticket,

        @NotNull(message = "凭证ID不能为空")
        Long providerId
) {
}
