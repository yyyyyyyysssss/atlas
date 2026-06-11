package com.atlas.auth.domain.dto;

import com.atlas.security.enums.ClientType;
import com.atlas.security.token.WebauthnAuthenticationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Web3LoginDTO(

        @NotNull(message = "客户端类型不能为空")
        ClientType clientType,

        @NotBlank(message = "事务id不能为空")
        String web3Id,

        @NotBlank(message = "签名不能为空")
        String signature
) {
}
