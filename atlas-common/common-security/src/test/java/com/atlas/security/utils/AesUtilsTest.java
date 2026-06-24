package com.atlas.security.utils;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class AesUtilsTest {

    @BeforeAll
    static void initEnv() {
        String userDir = System.getProperty("user.dir");
        String projectRoot = userDir.substring(0, userDir.indexOf("atlas") + "atlas".length());
        System.out.println(projectRoot);
        // 加载 .env 文件
        Dotenv dotenv = Dotenv.configure()
                .directory(projectRoot)
                .ignoreIfMissing()
                .load();
        String masterKey = dotenv.get("ATLAS_MASTER_KEY");
        if (masterKey != null) {
            System.setProperty("ATLAS_MASTER_KEY", masterKey);
        }
    }

    @Test
    void crypt() {
        String serviceName = "abc";
        String plainText = "123456";
        String key = KeyManager.deriveServiceKey(serviceName);

        String encrypt = AesUtils.encrypt(plainText, key);
        log.info("encrypt: {}", encrypt);

        String decrypt = AesUtils.decrypt(encrypt, key);
        log.info("decrypt: {}", decrypt);

        Assertions.assertEquals(decrypt, plainText);

    }
}