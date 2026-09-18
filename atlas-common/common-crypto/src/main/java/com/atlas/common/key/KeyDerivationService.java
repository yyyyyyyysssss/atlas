package com.atlas.common.key;

import com.atlas.common.crypto.exception.CryptoException;
import lombok.RequiredArgsConstructor;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/18 10:03
 */
@RequiredArgsConstructor
public class KeyDerivationService {

    private final String masterKey;


    public String deriveHex(String namespace) {

        return deriveHex(namespace ,32);
    }

    public String deriveHex(String namespace, int length) {
        if (namespace == null || namespace.isBlank()) {
            throw new CryptoException("key derive service namespace不能为空");
        }
        return KeyDerivation.deriveHex(
                namespace,
                masterKey,
                length
        );
    }

}
