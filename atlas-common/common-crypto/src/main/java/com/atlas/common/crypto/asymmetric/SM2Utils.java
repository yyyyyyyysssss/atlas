package com.atlas.common.crypto.asymmetric;

import com.atlas.common.crypto.exception.CryptoException;
import com.atlas.common.crypto.provider.BouncyCastleProviderHolder;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class SM2Utils {

    private SM2Utils() {
    }

    private static final String PROVIDER = BouncyCastleProviderHolder.providerName();

    private static final String EC = "EC";

    private static final String SIGN_ALGORITHM = "SM3withSM2";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * SM2加密
     *
     * @param plainText       明文
     * @param publicKeyBase64 公钥(Base64 DER)
     */
    public static String encrypt(String plainText, String publicKeyBase64) {
        check(plainText,"plainText");
        check(publicKeyBase64,"publicKey");
        try {
            PublicKey publicKey = decodePublicKey(publicKeyBase64);
            ECPublicKeyParameters params = (ECPublicKeyParameters) ECUtil.generatePublicKeyParameter(publicKey);

            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);

            engine.init(true, new ParametersWithRandom(params, SECURE_RANDOM));

            byte[] input = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] result = engine.processBlock(input, 0, input.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new CryptoException("SM2 encrypt error", e);
        }
    }

    /**
     * SM2解密
     */
    public static String decrypt(String cipherTextBase64, String privateKeyBase64) {
        check(cipherTextBase64,"cipherTextBase64");
        check(privateKeyBase64,"privateKeyBase64");
        try {
            PrivateKey privateKey = decodePrivateKey(privateKeyBase64);

            ECPrivateKeyParameters params = (ECPrivateKeyParameters) ECUtil.generatePrivateKeyParameter(privateKey);

            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);

            engine.init(false, params);

            byte[] cipher = Base64.getDecoder().decode(cipherTextBase64);

            byte[] result = engine.processBlock(cipher, 0, cipher.length);

            return new String(result, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new CryptoException("SM2 decrypt error", e);
        }
    }

    /**
     * SM2签名
     */
    public static String sign(String data, String privateKeyBase64) {
        check(data,"data");
        check(privateKeyBase64,"privateKeyBase64");
        try {

            PrivateKey privateKey = decodePrivateKey(privateKeyBase64);

            Signature signature = Signature.getInstance(SIGN_ALGORITHM, PROVIDER);
            signature.initSign(privateKey);

            signature.update(data.getBytes(StandardCharsets.UTF_8));

            byte[] result = signature.sign();

            return Base64.getEncoder().encodeToString(result);

        } catch (Exception e) {
            throw new CryptoException("SM2 sign error", e);
        }
    }




    /**
     * SM2验签
     */
    public static boolean verify(String data, String signBase64, String publicKeyBase64) {
        check(data,"data");
        check(signBase64,"signBase64");
        check(publicKeyBase64,"publicKeyBase64");
        try {
            PublicKey publicKey = decodePublicKey(publicKeyBase64);

            Signature signature = Signature.getInstance(SIGN_ALGORITHM, PROVIDER);

            signature.initVerify(publicKey);

            signature.update(data.getBytes(StandardCharsets.UTF_8));

            return signature.verify(Base64.getDecoder().decode(signBase64));

        } catch (Exception e) {
            throw new CryptoException("SM2 verify error", e);
        }
    }



    private static PublicKey decodePublicKey(String key) {
        try {
            byte[] bytes = Base64.getDecoder().decode(key);

            KeyFactory factory = KeyFactory.getInstance(EC, PROVIDER);

            return factory.generatePublic(new X509EncodedKeySpec(bytes));
        }catch (Exception e){
            throw new CryptoException("SM2 decodePublicKey error", e);
        }

    }

    private static PrivateKey decodePrivateKey(String key){
        try {
            byte[] bytes = Base64.getDecoder().decode(key);

            KeyFactory factory = KeyFactory.getInstance(EC, PROVIDER);

            return factory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
        }catch (Exception e){
            throw new CryptoException("SM2 decodePrivateKey error", e);
        }

    }

    private static void check(String value,String name){
        if(value==null || value.isBlank()){
            throw new CryptoException(name+" cannot be null");
        }
    }

}
