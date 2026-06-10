package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.Web3WalletType;

public record Web3WalletVerifySignatureResponse(
        String address,

        Web3WalletType walletType,

        String label,

        String source
) {
}
