package com.atlas.auth.domain.vo;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/11 10:06
 */
public record Web3WalletVerifyVO(
        boolean verified,

        String ticket
) {
}
