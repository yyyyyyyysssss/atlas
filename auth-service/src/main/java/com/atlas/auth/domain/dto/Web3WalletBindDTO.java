package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record Web3WalletBindDTO(

        @NotBlank(message = "安全验证凭证缺失，请重新进行身份验证")
        String ticket,

        @NotBlank(message = "事务ID不能为空")
        String web3Id,

        @NotBlank(message = "签名凭证不能为空")
        String signature
) {
}
