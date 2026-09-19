package com.atlas.common.crypto.asymmetric;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class RsaUtilsTest {


    @Test
    void crypt() {
        SecurityKeyPair securityKeyPair = RSAKeyGenerator.generate();
        String publicKey = securityKeyPair.publicKey();
        String privateKey = securityKeyPair.privateKey();
        log.info("publicKey: {}", publicKey);
        log.info("privateKey: {}", privateKey);

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