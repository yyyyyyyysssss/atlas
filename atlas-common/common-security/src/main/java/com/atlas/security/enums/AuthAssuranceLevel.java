package com.atlas.security.enums;

public enum AuthAssuranceLevel {

    /**
     * LEVEL 1: 单因素（弱信任）
     * 账密登录、短信登录、邮箱登录、第三方OAuth2
     */
    LOW(1),

    /**
     * LEVEL 2: 双因素（中信任）
     * 账密 + TOTP 验证码，或者账密 + 备份码
     */
    MEDIUM(2),

    /**
     * LEVEL 3: 硬件/生物级单因素或多因素（高信任）
     * FIDO2 / WebAuthn（指纹、面容、YubiKey 硬件密钥）
     */
    HIGH(3);

    private final int rank;

    AuthAssuranceLevel(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

}
