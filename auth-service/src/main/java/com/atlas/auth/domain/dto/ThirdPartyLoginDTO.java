package com.atlas.auth.domain.dto;

import com.atlas.security.enums.ClientType;
import jakarta.validation.constraints.NotNull;

public record ThirdPartyLoginDTO(

        @NotNull(message = "客户端类型不能为空")
        ClientType clientType,

        @NotNull(message = "用户id不能为空")
        Long userId
) {
}
