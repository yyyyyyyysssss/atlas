package com.atlas.common.key;

import com.atlas.common.crypto.digest.HmacUtils;
import com.atlas.common.crypto.exception.CryptoException;

/**
 * @Description 密钥派生工具 基于 HMAC-SHA256
 * @Author ys
 * @Date 2026/9/18 11:37
 */
public final class KeyDerivation {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private KeyDerivation() {
    }

    /**
     * 派生byte密钥
     *
     * @param namespace 命名空间
     * @param masterKey 主密钥
     * @return 32字节密钥
     */
    public static String deriveHex(String namespace, String masterKey) {
        validate(namespace, masterKey);
        return HmacUtils.hmac(namespace, masterKey, HMAC_ALGORITHM);
    }

    public static String deriveHex(String namespace, String masterKey, int length) {
        String hex = deriveHex(namespace, masterKey);
        if (length <= 0 || length > hex.length()) {
            throw new CryptoException("key derive invalid length");
        }
        return hex.substring(0, length);
    }

    private static void validate(String namespace, String masterKey) {
        if (namespace == null || namespace.isBlank()) {
            throw new CryptoException("key derive namespace不能为空");
        }

        if (masterKey == null || masterKey.isBlank()) {
            throw new CryptoException("key derive masterKey不能为空");
        }
    }

}
