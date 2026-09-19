package com.atlas.common.crypto.symmetric;

import com.atlas.common.crypto.key.KeyDerivationService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

        KeyDerivationService service = new KeyDerivationService("test-master-key");
        String key = service.deriveHex(serviceName);

        String encrypt = AESUtils.encrypt(plainText, key);
        log.info("encrypt: {}", encrypt);

        String decrypt = AESUtils.decrypt(encrypt, key);
        log.info("decrypt: {}", decrypt);

        Assertions.assertEquals(decrypt, plainText);

    }
}