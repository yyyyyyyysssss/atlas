package com.atlas.common.crypto.digest;

import com.atlas.common.crypto.exception.CryptoException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/18 14:10
 */
public final class HmacUtils {

    private HmacUtils() {
    }

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private static final String HMAC_SHA256 = "HmacSHA256";


    private static final String HMAC_SHA512 = "HmacSHA512";


    public static String hmacSha256(String data, String secret) {

        return HEX_FORMAT.formatHex(hmac(data.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
    }

    public static String hmacSha512(String data, String secret) {

        return HEX_FORMAT.formatHex(hmac(data.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA512));
    }

    public static String hmac(String data, String secret, String algorithm) {

        return HEX_FORMAT.formatHex(hmac(data.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8), algorithm));
    }

    public static byte[] hmac(byte[] data, byte[] key, String algorithm) {
        try {
            Mac mac = Mac.getInstance(algorithm);

            SecretKeySpec keySpec = new SecretKeySpec(key, algorithm);

            mac.init(keySpec);

            return mac.doFinal(data);
        } catch (Exception e) {
            throw new CryptoException("HMAC error ", e);
        }
    }

}
