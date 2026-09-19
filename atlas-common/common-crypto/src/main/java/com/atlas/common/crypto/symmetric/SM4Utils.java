package com.atlas.common.crypto.symmetric;

import com.atlas.common.crypto.exception.CryptoException;
import com.atlas.common.crypto.provider.BouncyCastleProviderHolder;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public final class SM4Utils {

    private SM4Utils() {
    }

    private static final String SM4 = "SM4";

    private static final String ALGORITHM = "SM4/GCM/NoPadding";

    private static final String PROVIDER = BouncyCastleProviderHolder.providerName();

    private static final int GCM_IV_LENGTH = 12;

    private static final int GCM_TAG_LENGTH = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();


    /**
     * 加密
     *
     * @param plainText 明文
     * @param secretKey 16字节密钥
     * @return Base64密文(包含IV)
     */
    public static String encrypt(String plainText, String secretKey) {
        validateKey(secretKey);

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM, PROVIDER);
            byte[] secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, SM4);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            /*
             * 密文结构:
             *
             * IV + CipherText + TAG
             *
             */
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());

        } catch (Exception e) {
            throw new CryptoException("SM4 Encryption Error ", e);
        }
    }

    /**
     * 解密
     *
     * @param encryptedBase64 Base64密文
     * @param secretKey       16字节密钥
     */
    public static String decrypt(String encryptedBase64, String secretKey) {
        validateKey(secretKey);
        try {
            byte[] data = Base64.getDecoder().decode(encryptedBase64);

            if(data.length <= GCM_IV_LENGTH){
                throw new CryptoException("invalid ciphertext");
            }

            ByteBuffer buffer = ByteBuffer.wrap(data);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM, PROVIDER);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), SM4);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] result = cipher.doFinal(encrypted);

            return new String(result, StandardCharsets.UTF_8);
        }catch (Exception e){
            throw new CryptoException("SM4 Decryption Error ", e);
        }
    }


    private static void validateKey(String key) {

        if (key == null || key.getBytes(StandardCharsets.UTF_8).length != 16) {

            throw new CryptoException("SM4 requires a 16-byte key");
        }
    }

}
