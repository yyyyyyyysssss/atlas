package com.atlas.security.utils;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
public class DigestUtils {

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    public static String sha256(String data) {

        return sha(data, "SHA-256");
    }

    public static String sha(String data, String algorithm) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(algorithm);
            byte[] digest = messageDigest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HEX_FORMAT.formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            log.error("sha error: ", e);
            throw new RuntimeException(e);
        }
    }

    public static String hmacSha256(String data, String key) {

        return hmac(data, key, "HmacSHA256");
    }

    public static String hmac(String data, String key, String algorithm) {
        return HEX_FORMAT.formatHex(hmacBytes(data, key, algorithm));
    }

    public static byte[] hmacBytes(String data, String key, String algorithm) {
        try {
            Mac hmac = Mac.getInstance(algorithm);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);
            hmac.init(secretKey);
            return hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("hmac 签名失败", e);
        }
    }

}
