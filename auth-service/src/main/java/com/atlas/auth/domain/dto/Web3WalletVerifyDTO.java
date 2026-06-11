package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.SecurityScene;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Web3WalletVerifyDTO(

        @NotBlank(message = "事务ID不能为空")
        String web3Id,

        @NotBlank(message = "签名凭证不能为空")
        String signature,

        @NotNull(message = "业务场景不能为空")
        SecurityScene securityScene
) {
}
