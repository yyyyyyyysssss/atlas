package com.atlas.security.utils;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.stream.Collectors;

@Slf4j
public class EncryptUtils {

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
        try {
            // 1. 创建 HMAC-SHA256 算法实例
            Mac hmac = Mac.getInstance(algorithm);

            // 2. 创建密钥对象
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);

            // 3. 初始化算法
            hmac.init(secretKey);

            // 4. 执行哈希运算并转换为 十六进制 (Hex)
            byte[] hashBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            return HEX_FORMAT.formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("hmac 签名失败", e);
        }
    }

    public static String base64Encode(String data) {

        return base64Encode(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String base64Encode(byte[] bytes) {

        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String base64Decode(String data) {
        byte[] decode = Base64.getDecoder().decode(data.getBytes(StandardCharsets.UTF_8));
        return new String(decode, StandardCharsets.UTF_8);
    }

    public static byte[] base64DecodeBytes(String data) {

        return Base64.getDecoder().decode(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String concatTokens(Object... parts) {
        if (parts == null || parts.length == 0) return "";
        return Arrays.stream(parts)
                .map(String::valueOf)
                .collect(Collectors.joining(":"));
    }

    public static String[] splitToken(String token) {
        if (token == null || token.isEmpty()) {
            return new String[0];
        }
        return token.split(":", -1);
    }

}
