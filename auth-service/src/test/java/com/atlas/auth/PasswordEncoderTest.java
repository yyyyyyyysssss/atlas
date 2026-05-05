package com.atlas.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.apereo.cas.client.util.CommonUtils.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void testPasswordEncodingAndMatching() {
        String rawPassword = "45546ecf428b4f86adfa93798d82dca4";

        // 1. 生成加密后的密文
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // 2. 打印看看（你会发现每次运行结果都不同）
        System.out.println("Encoded Password: " + encodedPassword);

        // 原始密码与加密后的密文是否匹配
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword), "密码应该匹配成功");

        // 错误的密码应该匹配失败
        assertFalse(passwordEncoder.matches("wrong_password", encodedPassword), "错误的密码不应匹配成功");
    }

}
