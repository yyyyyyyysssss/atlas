package com.atlas.common.crypto.asymmetric;

import com.atlas.common.crypto.exception.CryptoException;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/18 15:11
 */
public final class RSAKeyLoader {

    private RSAKeyLoader(){}

    /**
     * 默认环境变量
     */
    private static final String DEFAULT_PUBLIC_KEY_ENV = "ATLAS_PUBLIC_KEY";


    private static final String DEFAULT_PRIVATE_KEY_ENV = "ATLAS_PRIVATE_KEY";

    // Base64字符串加载公钥
    public static PublicKey loadPublicKey(String key){
        try {

            byte[] bytes = Base64.getDecoder().decode(key);

            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);

            return KeyFactory.getInstance("RSA").generatePublic(spec);

        }catch(Exception e){
            throw new CryptoException("RSA load public key failed", e);
        }
    }

    // Base64字符串加载私钥
    public static PrivateKey loadPrivateKey(String key){
        try {

            byte[] bytes = Base64.getDecoder().decode(key);

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);

            return KeyFactory.getInstance("RSA").generatePrivate(spec);

        }catch(Exception e){
            throw new CryptoException("RSA load private key failed", e);
        }

    }

    // 从环境变量加载公钥
    public static PublicKey loadDefaultPublicKey() {
        String value = System.getenv(DEFAULT_PUBLIC_KEY_ENV);
        if(value == null || value.isBlank()){
            throw new CryptoException("RSA public key env not found: " + DEFAULT_PUBLIC_KEY_ENV);
        }
        return loadPublicKey(value);
    }

    // 从环境变量加载私钥
    public static PrivateKey loadDefaultPrivateKey() {
        String value = System.getenv(DEFAULT_PRIVATE_KEY_ENV);
        if(value == null || value.isBlank()){
            throw new CryptoException("RSA private key env not found: " + DEFAULT_PRIVATE_KEY_ENV);
        }
        return loadPrivateKey(value);
    }

}
