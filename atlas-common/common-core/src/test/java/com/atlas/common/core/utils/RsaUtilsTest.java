package com.atlas.common.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class RsaUtilsTest {


    @Test
    void crypt() throws Exception {
        RsaUtils.KeyPairValue keyPairValue = RsaUtils.generateKeyPair();
        String publicKey = keyPairValue.getPublicKey();
        String privateKey = keyPairValue.getPrivateKey();
        log.info("publicKey: {}", publicKey);
        log.info("privateKey: {}", privateKey);

        String plainText = "data";
        String encrypt = RsaUtils.encrypt(plainText, publicKey);
        log.info("encrypt: {}", encrypt);

        String decrypt = RsaUtils.decrypt(encrypt, privateKey);
        log.info("decrypt: {}", decrypt);

        String sign = RsaUtils.sign(plainText, privateKey);
        log.info("sign: {}", sign);

        boolean verify = RsaUtils.verify(plainText, publicKey, sign);
        log.info("signVerify: {}", verify);

        Assertions.assertEquals(decrypt, plainText);

        Assertions.assertTrue(verify);

    }

}