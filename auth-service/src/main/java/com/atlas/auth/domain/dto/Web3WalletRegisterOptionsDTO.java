package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.Web3WalletType;
import jakarta.validation.constraints.NotBlank;

public record Web3WalletRegisterOptionsDTO(

        String ticket,

        @NotBlank(message = "钱包地址不能为空")
        String address,

        Web3WalletType walletType,

        String label,

        String source
) {
}
