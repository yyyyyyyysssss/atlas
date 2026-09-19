package com.atlas.common.crypto.asymmetric;

import com.atlas.common.crypto.exception.CryptoException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/18 15:07
 */
public final class RSAKeyGenerator {

    private RSAKeyGenerator(){}


    public static SecurityKeyPair generate(){

        return generate(2048);
    }

    public static SecurityKeyPair generate(int keySize){
        if(keySize != 2048 && keySize !=3072 && keySize !=4096){
            throw new CryptoException("RSA invalid key size");
        }
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(keySize);
            KeyPair keyPair = generator.generateKeyPair();
            String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            return new SecurityKeyPair(publicKey, privateKey);
        }catch (Exception e){
            throw new CryptoException("RSA generateKeyPair error ", e);
        }
    }
}
