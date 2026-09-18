package com.atlas.common.crypto.symmetric;

import com.atlas.common.crypto.exception.CryptoException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES对称加密工具
 * 默认:
 * AES/GCM/NoPadding
 * 特点:
 * - 加密
 * - 完整性校验
 * - 防篡改
 */
public final class AESUtils {

    private AESUtils() {
    }

    private static final String AES = "AES";

    // AES-GCM模式
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    // IV长度
    private static final int GCM_IV_LENGTH = 12;

    // GCM认证长度
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
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), AES);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);

            return Base64.getEncoder().encodeToString(byteBuffer.array());

        }catch (Exception e){
            throw new CryptoException("AES Encryption Error ", e);
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
            throw new CryptoException("AES Decryption Error ", e);
        }
    }

    private static void validateKey(String key) {
        if (key == null || key.length() != 32) {
            throw new CryptoException("AES-256 requires a 32-character key");
        }
    }
}
