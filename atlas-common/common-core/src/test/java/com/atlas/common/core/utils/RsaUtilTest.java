package com.atlas.common.core.utils;

import org.junit.jupiter.api.Test;
import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class RsaUtilTest {

    private static final String TARGET_DATA = "{\"id\":123,\"name\":\"xxx\"}";

    @Test
    void getKeyPair() throws Exception {
        RsaUtils.KeyPairValue keyPairValue = RsaUtils.generateKeyPair();
        String publicKey = keyPairValue.getPublicKey();
        String privateKey = keyPairValue.getPrivateKey();
        log.info("generate publicKey: {}", publicKey);
        log.info("generate privateKey: {}", privateKey);
        String encrypt = RsaUtils.encrypt(TARGET_DATA, publicKey, RsaUtils.PaddingMode.OAEP_SHA1);
        String decrypt = RsaUtils.decrypt(encrypt, privateKey, RsaUtils.PaddingMode.OAEP_SHA1);
        assertEquals(decrypt, TARGET_DATA);
    }

    @Test
    void encrypt() throws Exception {
        RsaUtils.KeyPairValue keyPairValue = RsaUtils.generateKeyPair();
        String publicKey = keyPairValue.getPublicKey();
        String encrypt = RsaUtils.encrypt(TARGET_DATA,publicKey,RsaUtils.PaddingMode.OAEP_MD5);
        log.info("encrypt : {}", encrypt);
        assertNotNull(encrypt);
    }

    @Test
    void decrypt() throws Exception {
        RsaUtils.KeyPairValue keyPairValue = RsaUtils.generateKeyPair();
        String publicKey = keyPairValue.getPublicKey();
        String privateKey = keyPairValue.getPrivateKey();
        String encrypt = RsaUtils.encrypt(TARGET_DATA, publicKey, RsaUtils.PaddingMode.OAEP_MD5);
        String decrypt = RsaUtils.decrypt(encrypt, privateKey, RsaUtils.PaddingMode.OAEP_MD5);
        log.info("decrypt : {}", decrypt);
        assertNotNull(decrypt);
    }

    @Test
    void sign() throws Exception {
        RsaUtils.KeyPairValue keyPairValue = RsaUtils.generateKeyPair();
        String privateKey = keyPairValue.getPrivateKey();
        String sign = RsaUtils.sign(TARGET_DATA,privateKey);
        log.info("sign : {}", sign);
        assertNotNull(sign);
    }

    @Test
    void verify() throws Exception {
        RsaUtils.KeyPairValue keyPairValue = RsaUtils.generateKeyPair();
        String publicKey = keyPairValue.getPublicKey();
        String privateKey = keyPairValue.getPrivateKey();
        String sign = RsaUtils.sign(TARGET_DATA, privateKey);
        boolean verify = RsaUtils.verify(TARGET_DATA, publicKey, sign);
        log.info("verify : {}", verify);
        assertTrue(verify);
    }

    @Test
    void loadLocalPrivateKey() {
        String privateKey = RsaUtils.loadLocalPrivateKeyStr();
        log.info("privateKey: {}", privateKey);
    }

    @Test
    void loadLocalPublicKey() {
        String publicKey = RsaUtils.loadLocalPublicKeyStr();
        log.info("publicKey: {}", publicKey);
    }

}