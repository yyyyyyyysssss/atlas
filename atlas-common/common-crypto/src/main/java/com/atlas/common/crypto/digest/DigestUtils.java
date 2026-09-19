package com.atlas.common.crypto.digest;

import com.atlas.common.crypto.exception.CryptoException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

/**
 * 摘要工具类
 * 支持:
 * MD5
 * SHA-1
 * SHA-256
 * SHA-512
 */
public final class DigestUtils {

    private DigestUtils() {

    }

    private static final String MD5 = "MD5";

    private static final String SHA1 = "SHA-1";

    private static final String SHA256 = "SHA-256";

    private static final String SHA512 = "SHA-512";

    private static final Set<String> SUPPORT_ALGORITHMS = Set.of(
            MD5,
            SHA1,
            SHA256,
            SHA512
    );

    public static String md5(String data) {

        return HexFormat.of().formatHex(digest(data.getBytes(StandardCharsets.UTF_8), MD5));
    }

    public static String md5(byte[] data) {

        return HexFormat.of().formatHex(digest(data, MD5));
    }

    public static String md5(InputStream inputStream) {

        return HexFormat.of().formatHex(digest(inputStream, MD5));
    }

    public static String sha1(String data) {

        return HexFormat.of().formatHex(digest(data.getBytes(StandardCharsets.UTF_8), SHA1));
    }

    public static String sha1(byte[] data) {

        return HexFormat.of().formatHex(digest(data, SHA1));
    }

    public static String sha1(InputStream inputStream) {

        return HexFormat.of().formatHex(digest(inputStream, SHA1));
    }

    public static String sha256(String data) {

        return HexFormat.of().formatHex(digest(data.getBytes(StandardCharsets.UTF_8), SHA256));
    }

    public static String sha256(byte[] data) {

        return HexFormat.of().formatHex(digest(data, SHA256));
    }

    public static String sha256(InputStream inputStream) {

        return HexFormat.of().formatHex(digest(inputStream, SHA256));
    }

    public static String sha512(String data) {

        return HexFormat.of().formatHex(digest(data.getBytes(StandardCharsets.UTF_8), SHA512));
    }

    public static String sha512(byte[] data) {

        return HexFormat.of().formatHex(digest(data, SHA512));
    }

    public static String sha512(InputStream inputStream) {

        return HexFormat.of().formatHex(digest(inputStream, SHA512));
    }

    // 文件 SHA-256
    public static String sha256(File file) {
        if (file == null || !file.exists()) {
            throw new CryptoException("file not exists");
        }

        try (InputStream inputStream = new FileInputStream(file)) {

            return HexFormat.of().formatHex(digest(inputStream, SHA256));
        } catch (Exception e) {
            throw new CryptoException("File digest error ", e);
        }
    }

    public static byte[] digest(byte[] data, String algorithm) {
        checkAlgorithm(algorithm);
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            return md.digest(data);
        } catch (Exception e) {
            throw new CryptoException("digest error", e);
        }
    }

    public static byte[] digest(InputStream inputStream, String algorithm) {
        checkAlgorithm(algorithm);
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, length);
            }
            return messageDigest.digest();
        } catch (Exception e) {
            throw new CryptoException("Digest error", e);
        }
    }

    private static void checkAlgorithm(String algorithm){

        if(!SUPPORT_ALGORITHMS.contains(algorithm)){
            throw new CryptoException("Unsupported digest algorithm: " + algorithm);
        }
    }

}
