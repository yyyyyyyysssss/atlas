package com.atlas.common.core.utils;

import java.security.SecureRandom;
import java.util.Random;


public class VerificationCodeUtils {

    private static final char[] META_DATA = {
            '0', '1', '2', '3', '4',
            '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E',
            'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O',
            'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y',
            'Z'
    };

    private static final Random RANDOM = new SecureRandom();

    public static String genVerificationCode(){
        return genVerificationCode(6);
    }

    /**
     * 生成指定位数的纯数字验证码
     * @param digit 验证码位数
     * @return 纯数字验证码字符串
     */
    public static String genVerificationCode(int digit) {
        if (digit <= 0) {
            throw new IllegalArgumentException("验证码位数必须大于 0");
        }

        char[] chars = new char[digit];
        for (int i = 0; i < digit; i++) {
            // 💡 直接限定在 0 - 9 的 ASCII 字符区间：'0' 的 ASCII 码是 48
            chars[i] = (char) ('0' + RANDOM.nextInt(10));
        }
        return new String(chars);
    }

}
