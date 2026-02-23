package com.atlas.security.utils;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
public class EncryptUtils {

    public static String sha256(String data) {
        String waitEncryptStr = getWaitEncryptStr(data);
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(waitEncryptStr.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            log.error("getSHA256Str error: ", e);
            throw new RuntimeException(e);
        }
    }

    public static String hmacSha256(String data, String key) {
        try {
            // 1. 创建 HMAC-SHA256 算法实例
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");

            // 2. 创建密钥对象
            SecretKeySpec secretKey = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            );

            // 3. 初始化算法
            sha256Hmac.init(secretKey);

            // 4. 执行哈希运算并转换为 十六进制 (Hex)
            byte[] hashBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("HmacSHA256 签名失败", e);
        }
    }

    public static String base64Encode(String data) {
        byte[] encode = Base64.getEncoder().encode(data.getBytes(StandardCharsets.UTF_8));
        return new String(encode,StandardCharsets.UTF_8);
    }

    public static String base64Encode(byte[] bytes) {
        byte[] encode = Base64.getEncoder().encode(bytes);
        return new String(encode,StandardCharsets.UTF_8);
    }

    public static String base64Decode(String data) {
        byte[] decode = Base64.getDecoder().decode(data.getBytes(StandardCharsets.UTF_8));
        return new String(decode,StandardCharsets.UTF_8);
    }
    public static byte[] base64DecodeBytes(String data) {
        return Base64.getDecoder().decode(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String getWaitEncryptStr(String... data){
        if (data == null || data.length == 0) {
            return "";
        }
        return String.join(":", data);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
