package com.atlas.security.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/18 9:36
 */
public class AesUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private static final int GCM_IV_LENGTH = 12;

    private static final int GCM_TAG_LENGTH = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();



    /**
     * 加密数据
     * @param plainText 明文
     * @param secretKey 32字节(256位)的密钥
     * @return Base64编码后的密文(包含IV)
     */
    public static String encrypt(String plainText, String secretKey) {
        validateKey(secretKey);
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);

            return Base64.getEncoder().encodeToString(byteBuffer.array());

        }catch (Exception e){
            throw new SecurityException("AES Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * 解密数据
     * @param encryptedBase64 包含IV的Base64密文
     * @param secretKey 32字节(256位)密钥
     * @return 原始明文
     */
    public static String decrypt(String encryptedBase64, String secretKey) {
        validateKey(secretKey);
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv); // 读取前12字节作为 IV
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted); // 读取剩余内容作为密文

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SecurityException("AES Decryption failed: " + e.getMessage(), e);
        }
    }

    private static void validateKey(String key) {
        if (key == null || key.length() != 32) {
            throw new IllegalArgumentException("AES-256 requires a 32-character key.");
        }
    }
}
