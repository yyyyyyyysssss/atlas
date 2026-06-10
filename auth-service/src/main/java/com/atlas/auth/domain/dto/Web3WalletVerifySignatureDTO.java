package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record Web3WalletVerifySignatureDTO(
        @NotBlank(message = "事务ID不能为空")
        String web3Id,

        @NotBlank(message = "签名凭证不能为空")
        String signature
) {
}
