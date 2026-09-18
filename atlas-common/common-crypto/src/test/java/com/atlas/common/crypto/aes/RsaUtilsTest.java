package com.atlas.common.crypto.aes;

import com.atlas.common.crypto.asymmetric.RSAKeyGenerator;
import com.atlas.common.crypto.asymmetric.RSAKeyPair;
import com.atlas.common.crypto.asymmetric.RSAUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class RsaUtilsTest {


    @Test
    void crypt() {
        RSAKeyPair rsaKeyPair = RSAKeyGenerator.generate();
        String publicKey = rsaKeyPair.publicKey();
        String privateKey = rsaKeyPair.privateKey();
        log.info("publicKey: {}", publicKey);
        log.info("privateKey: {}", privateKey);

        String plainText = "data";
        String encrypt = RSAUtils.encrypt(plainText, publicKey);
        log.info("encrypt: {}", encrypt);

        String decrypt = RSAUtils.decrypt(encrypt, privateKey);
        log.info("decrypt: {}", decrypt);

        String sign = RSAUtils.sign(plainText, privateKey);
        log.info("sign: {}", sign);

        boolean verify = RSAUtils.verify(plainText, publicKey, sign);
        log.info("signVerify: {}", verify);

        Assertions.assertEquals(decrypt, plainText);

        Assertions.assertTrue(verify);

    }

}