package com.atlas.common.crypto.asymmetric;

import com.atlas.common.crypto.exception.CryptoException;
import com.atlas.common.crypto.provider.BouncyCastleProviderHolder;
import org.bouncycastle.jce.spec.ECNamedCurveGenParameterSpec;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Base64;

public final class SM2KeyGenerator {

    private SM2KeyGenerator(){}

    private static final String PROVIDER = BouncyCastleProviderHolder.providerName();

    private static final String KEY_ALGORITHM = "EC";

    private static final String CURVE = "sm2p256v1";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();


    public static SecurityKeyPair generate(){
        try {

            KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM, PROVIDER);
            generator.initialize(new ECNamedCurveGenParameterSpec(CURVE), SECURE_RANDOM);
            KeyPair keyPair = generator.generateKeyPair();
            String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            return new SecurityKeyPair(publicKey, privateKey);
        }catch(Exception e){
            throw new CryptoException("SM2 generate key error", e);
        }
    }

}
