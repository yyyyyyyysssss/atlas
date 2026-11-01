package com.atlas.common.crypto.digest;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class DigestUtilsTest {


    @Test
    void testSha256() {
        String result = DigestUtils.sha256("http://localhost:3000/cas/callback/atlas");
        log.info("{}", result);
        assertNotNull(result);
    }

}
