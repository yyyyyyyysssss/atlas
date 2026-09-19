package com.atlas.common.crypto.symmetric;

import com.atlas.common.crypto.exception.CryptoException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class SM4UtilsTest {


    /**
     * SM4固定128bit密钥
     * 16字节
     */
    private static final String KEY = "0123456789abcdef";


    @Test
    void testEncryptAndDecrypt() {

        String plainText = "hello sm4 国密算法";

        String encrypted = SM4Utils.encrypt(plainText, KEY);

        log.info("SM4 encrypt: {}", encrypted);

        assertNotNull(encrypted);

        String decrypt = SM4Utils.decrypt(encrypted, KEY);

        log.info("SM4 decrypt: {}", decrypt);

        assertEquals(plainText, decrypt);
    }



    @Test
    void testEncryptResultDifferent() {

        String plainText = "same message";


        String encrypted1 = SM4Utils.encrypt(plainText, KEY);

        String encrypted2 = SM4Utils.encrypt(plainText, KEY);

        /*
         * 因为IV随机
         * 所以密文应该不同
         */
        assertNotEquals(encrypted1, encrypted2);


        assertEquals(plainText, SM4Utils.decrypt(encrypted1, KEY));

        assertEquals(plainText, SM4Utils.decrypt(encrypted2, KEY));
    }

}