package com.atlas.security.utils;

import lombok.extern.slf4j.Slf4j;

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

}
