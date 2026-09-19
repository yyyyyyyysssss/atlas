package com.atlas.common.crypto.asymmetric;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class SM2KeyGeneratorTest {

    @Test
    void testGenerate() {

        SecurityKeyPair pair = SM2KeyGenerator.generate();

        log.info("publicKey: {}", pair.publicKey());
        log.info("privateKey: {}", pair.privateKey());

        assertNotNull(pair.publicKey());
        assertNotNull(pair.privateKey());

        assertFalse(pair.publicKey().isEmpty());
        assertFalse(pair.privateKey().isEmpty());
    }

}