package com.atlas.common.crypto.asymmetric;

import com.atlas.common.crypto.exception.CryptoException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAKey;
import java.util.Base64;

/**
 * RSA 加密工具
 * 默认:
 * RSA-OAEP-SHA256
 */
@Slf4j
public final class RSAUtils {

    private RSAUtils(){}
    /**
     * 编码
     */
    private static final String charset = "utf-8";

    private static final PaddingMode DEFAULT_PADDING = PaddingMode.OAEP_SHA256;

    private static final SignAlgorithm DEFAULT_SIGN = SignAlgorithm.SHA256withRSA;


    //加密
    public static String encrypt(String context) {
        PublicKey publicKey = RSAKeyLoader.loadDefaultPublicKey();
        return encrypt(context, publicKey, DEFAULT_PADDING);
    }

    public static String encrypt(String context, String publicKey) {
        return encrypt(context, publicKey, DEFAULT_PADDING);
    }

    public static String encrypt(String context, String publicKey, PaddingMode paddingMode) {
        PublicKey pk = RSAKeyLoader.loadPublicKey(publicKey);
        return encrypt(context, pk, paddingMode);
    }

    public static String encrypt(String context, PublicKey publicKey, PaddingMode paddingMode) {
        try {
            Cipher cipher = Cipher.getInstance(paddingMode.getCode());
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] bytes = context.getBytes(StandardCharsets.UTF_8);
            byte[] crypt = crypt(bytes, cipher, getEncryptBlock(publicKey, paddingMode));
            return Base64.getEncoder().encodeToString(crypt);
        }catch (Exception e){
            throw new CryptoException("RSA encrypt error " , e);
        }
    }


    //解密
    public static String decrypt(String context) {
        PrivateKey privateKey = RSAKeyLoader.loadDefaultPrivateKey();
        return decrypt(context, privateKey, DEFAULT_PADDING);
    }

    public static String decrypt(String context, String privateKey) {
        return decrypt(context, privateKey, DEFAULT_PADDING);
    }

    public static String decrypt(String context, String privateKey, PaddingMode paddingMode) {
        PrivateKey pk = RSAKeyLoader.loadPrivateKey(privateKey);
        return decrypt(context, pk, paddingMode);
    }

    public static String decrypt(String context, PrivateKey privateKey, PaddingMode paddingMode) {
        try {
            Cipher cipher = Cipher.getInstance(paddingMode.getCode());
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] bytes = Base64.getDecoder().decode(context);
            byte[] crypt = crypt(bytes, cipher, getDecryptBlock(privateKey));
            return new String(crypt, StandardCharsets.UTF_8);
        }catch (Exception e){
            throw new CryptoException("RSA decrypt error ",  e);
        }
    }

    private static byte[] crypt(byte[] dataBytes, Cipher cipher, int maxBlock) {
        int inputLen = dataBytes.length;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()){
            int offset = 0;
            byte[] cache;
            int i = 0;
            // 对数据分段解密
            while (inputLen - offset > 0) {
                if (inputLen - offset > maxBlock) {
                    cache = cipher.doFinal(dataBytes, offset, maxBlock);
                } else {
                    cache = cipher.doFinal(dataBytes, offset, inputLen - offset);
                }
                out.write(cache, 0, cache.length);
                i++;
                offset = i * maxBlock;
            }
            return out.toByteArray();
        } catch (Exception e){
            throw new CryptoException("RSA crypt error ", e);
        }
    }


    public static String sign(String context){
        PrivateKey privateKey = RSAKeyLoader.loadDefaultPrivateKey();
        return sign(context, privateKey, DEFAULT_SIGN);
    }

    public static String sign(String context, String privateKey){
        PrivateKey pk = RSAKeyLoader.loadPrivateKey(privateKey);
        return sign(context, pk, DEFAULT_SIGN);
    }

    public static String sign(String context, PrivateKey privateKey, SignAlgorithm signAlgorithm){
        try {
            byte[] bytes = context.getBytes(charset);
            Signature signature = Signature.getInstance(signAlgorithm.getCode());
            signature.initSign(privateKey);
            signature.update(bytes);
            return new String(Base64.getEncoder().encode(signature.sign()), charset);
        }catch (Exception e){
            throw new CryptoException("RSA sign error ", e);
        }
    }

    public static boolean verify(String context, String sign){
        PublicKey publicKey = RSAKeyLoader.loadDefaultPublicKey();
        return verify(context, publicKey, sign, DEFAULT_SIGN);
    }

    public static boolean verify(String context, String publicKey, String sign) {
        PublicKey pk = RSAKeyLoader.loadPublicKey(publicKey);
        return verify(context, pk, sign, DEFAULT_SIGN);
    }

    public static boolean verify(String context, PublicKey publicKey, String sign, SignAlgorithm signAlgorithm) {
        try {
            byte[] srcBytes = context.getBytes(charset);
            byte[] signBytes = sign.getBytes(charset);
            Signature signature = Signature.getInstance(signAlgorithm.getCode());
            signature.initVerify(publicKey);
            signature.update(srcBytes);
            return signature.verify(Base64.getDecoder().decode(signBytes));
        }catch (Exception e){
            throw new CryptoException("RSA verify error ", e);
        }
    }


    private static int getEncryptBlock(PublicKey key, PaddingMode mode){
        int keyBytes = ((RSAKey)key).getModulus().bitLength() / 8;
        return switch (mode) {
            case OAEP_SHA256 -> keyBytes - 2 * 32 - 2;
            case OAEP_SHA1 -> keyBytes - 2 * 20 - 2;
            case OAEP_MD5 -> keyBytes - 2 * 16 - 2;
            case PKCS1_PADDING -> keyBytes - 11;
        };
    }

    private static int getDecryptBlock(PrivateKey key){

        return ((RSAKey)key).getModulus().bitLength() / 8;
    }


    public enum PaddingMode {
        OAEP_MD5("RSA/ECB/OAEPWithMD5AndMGF1Padding"),
        OAEP_SHA1("RSA/ECB/OAEPWithSHA1AndMGF1Padding"),
        OAEP_SHA256("RSA/ECB/OAEPWithSHA-256AndMGF1Padding"),
        PKCS1_PADDING("RSA/ECB/PKCS1Padding"),
        ;
        private String code;

        public String getCode() {
            return code;
        }

        PaddingMode(String code) {
            this.code = code;
        }
    }


    public enum SignAlgorithm {
        SHA1withRSA("SHA1withRSA"),
        SHA256withRSA("SHA256withRSA"),
        MD5withRSA("MD5withRSA");
        private String code;

        public String getCode() {
            return code;
        }

        SignAlgorithm(String code) {
            this.code = code;
        }
    }

}
