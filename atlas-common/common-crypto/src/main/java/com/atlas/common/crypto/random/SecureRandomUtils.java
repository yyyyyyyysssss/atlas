package com.atlas.common.crypto.random;

import com.atlas.common.crypto.exception.CryptoException;

import java.security.SecureRandom;
import java.util.Base64;

public final class SecureRandomUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int DEFAULT_BYTE_LENGTH = 32;

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private static final char[] CODE_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();

    private static final int MAX_LENGTH = 1024;

    private SecureRandomUtils() {
    }

    public static String generate() {
        return generate(DEFAULT_BYTE_LENGTH);
    }

    /**
     * 生成指定长度的安全随机字符串
     *
     * @param byteLength 随机字节长度
     *                   长度越大随机空间越大
     * 建议:
     * - 普通token >= 16 bytes
     * - 安全凭证 >= 32 bytes
     */
    public static String generate(int byteLength) {

        validateLength(byteLength);

        // 1. 分配指定大小的字节数组
        byte[] randomBytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(randomBytes);

        // 2. 使用 URL 安全的 Base64 编码，去掉末尾占位符
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * 生成Hex随机字符串
     * 示例:
     * generateHex(8)
     * ab39f201
     *
     * @param length hex长度
     */
    public static String generateHex(int length) {

        validateLength(length);

        byte[] randomBytes = new byte[(length + 1) / 2];
        SECURE_RANDOM.nextBytes(randomBytes);
        StringBuilder builder = new StringBuilder(length);
        for (byte b : randomBytes) {
            builder.append(HEX_DIGITS[(b >> 4) & 0x0F]);
            builder.append(HEX_DIGITS[b & 0x0F]);
        }
        return builder.substring(0, length);
    }

    /**
     * 生成随机数字+小写字母编码
     * 适合：
     * - 文件分享码
     * - 邀请码
     *
     * @param length 长度
     */
    public static String generateCode(int length) {

        validateLength(length);

        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int i1 = SECURE_RANDOM.nextInt(CODE_CHARS.length);
            builder.append(CODE_CHARS[i1]);
        }
        return builder.toString();
    }

    private static void validateLength(int length) {
        if (length <= 0) {
            throw new CryptoException("Length must be positive");
        }
        if (length > MAX_LENGTH) {
            throw new CryptoException("invalid length");
        }
    }

    public static void main(String[] args) {
        System.out.println(SecureRandomUtils.generate());
        System.out.println(SecureRandomUtils.generateHex(8));
        System.out.println(SecureRandomUtils.generateCode(8));
    }

}
