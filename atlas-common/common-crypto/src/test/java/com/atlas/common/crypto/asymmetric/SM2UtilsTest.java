package com.atlas.common.crypto.asymmetric;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class SM2UtilsTest {


    @Test
    void testEncryptAndDecrypt() {

        SecurityKeyPair keyPair = SM2KeyGenerator.generate();

        log.info("publicKey: {}", keyPair.publicKey());

        log.info("privateKey: {}", keyPair.privateKey());

        String plainText = "{\n" +
                "  \"userId\": 10086,\n" +
                "  \"username\": \"security_test_user\",\n" +
                "  \"email\": \"test.rsa@example.com\",\n" +
                "  \"isActive\": true,\n" +
                "  \"roles\": [\"ROLE_ADMIN\", \"ROLE_USER\"],\n" +
                "  \"metadata\": {\n" +
                "    \"loginIp\": \"192.168.1.100\",\n" +
                "    \"timestamp\": 1774051528\n" +
                "  },\n" +
                "  \"secretPayload\": \"这是一段包含中文和特殊符号的敏感测试数据。\"\n" +
                "}";

        String cipherText = SM2Utils.encrypt(plainText, keyPair.publicKey());

        log.info("cipherText: {}", cipherText);


        String result = SM2Utils.decrypt(cipherText, keyPair.privateKey());

        log.info("decrypt result: {}", result);

        assertEquals(plainText, result);

    }

    @Test
    void testSignAndVerify() {

        SecurityKeyPair keyPair = SM2KeyGenerator.generate();

        String data = "hello sm2 signature";

        String sign = SM2Utils.sign(data, keyPair.privateKey());

        log.info("signature: {}", sign);

        boolean verify = SM2Utils.verify(data, sign, keyPair.publicKey());

        assertTrue(verify);

    }

}