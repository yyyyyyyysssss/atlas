package com.atlas.common.crypto.digest;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class SM3UtilsTest {


    private static final String SM3_ABC = "66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0";


    @Test
    void testDigestString() {
        String result = SM3Utils.digestHex("abc");
        log.info("{}", result);
        assertEquals(SM3_ABC, result);
    }


    @Test
    void testDigestBytes() {
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        String result = SM3Utils.digestHex(data);
        log.info("{}", result);
        assertEquals(SM3_ABC, result);
    }


    @Test
    void testDigestInputStream() {
        ByteArrayInputStream input = new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8));
        byte[] digest = SM3Utils.digestInputStream(input);
        String result = HexFormat.of().formatHex(digest);
        log.info("{}", result);
        assertEquals(SM3_ABC, result);
    }


    @Test
    void testDigestEmpty() {
        String result = SM3Utils.digestHex("");
        assertNotNull(result);
        assertEquals(64, result.length());
    }


    @Test
    void testNullString() {
        assertThrows(RuntimeException.class, () -> SM3Utils.digestHex((String)null));
    }


    @Test
    void testNullBytes() {
        assertThrows(RuntimeException.class, () -> SM3Utils.digest((byte[])null));
    }
}