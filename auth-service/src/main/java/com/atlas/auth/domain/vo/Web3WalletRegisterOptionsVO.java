package com.atlas.auth.domain.vo;

public record Web3WalletRegisterOptionsVO(

        // 随机挑战码 作用：防重放攻击，保证每一次签名请求的唯一性
        String challenge,

        // 前端提交绑定时携带该 ID，后端根据 ID 快速定位到缓存中的 Nonce 记录
        String web3Id,

        // 签名原文
        String message,

        Long expiresAt

) {
}
