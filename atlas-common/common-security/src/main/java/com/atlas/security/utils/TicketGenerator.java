package com.atlas.security.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class TicketGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int DEFAULT_BYTE_LENGTH = 32;

    private TicketGenerator() {}

    public static String generate() {
        return generate(DEFAULT_BYTE_LENGTH);
    }

    /**
     * 生成指定强度的安全票据
     * * @param byteLength 原始随机数的字节数。字节数越多，安全性越高，生成的字符串越长。
     * 建议：安全凭证 >= 32; 普通短令牌 >= 16
     */
    public static String generate(int byteLength) {
        if (byteLength <= 0) {
            throw new IllegalArgumentException("Byte length must be positive");
        }

        // 1. 分配指定大小的字节数组
        byte[] randomBytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(randomBytes);

        // 2. 使用 URL 安全的 Base64 编码，去掉末尾占位符
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

}
