package com.atlas.common.core.crypto;

import com.atlas.common.core.utils.DigestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HexFormat;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/18 10:03
 */
@Component
@RequiredArgsConstructor
public class KeyDerivationService {

    private final KeyProperties keyProperties;


    public String derive(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace不能为空");
        }
        String masterKey = keyProperties.getMasterKey();

        if (masterKey == null || masterKey.isBlank()) {
            throw new IllegalStateException("atlas.crypto.master-key 未配置");
        }
        byte[] hash = DigestUtils.hmacBytes(namespace, masterKey, "HmacSHA256");
        return HexFormat.of().formatHex(hash).substring(0, 32);
    }

}
