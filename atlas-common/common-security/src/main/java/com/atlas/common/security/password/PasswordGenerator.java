package com.atlas.common.security.password;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/6 20:04
 */
public final class PasswordGenerator {

    private PasswordGenerator(){}

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 默认策略
     */
    private static final PasswordPolicy DEFAULT_POLICY =
            PasswordPolicy.builder()
                    .length(12)
                    .uppercase(true)
                    .lowercase(true)
                    .digit(true)
                    .special(true)
                    .build();

    // 使用默认策略生成密码
    public static String generate() {
        return generate(DEFAULT_POLICY);
    }

    public static String generate(PasswordPolicy policy) {
        if(policy.getLength() < 4){
            throw new IllegalArgumentException("密码长度不能小于4");
        }
        List<Character> chars = new ArrayList<>();

        if(policy.isUppercase()){
            chars.add(randomCharFrom(UPPER));
        }

        if(policy.isLowercase()){
            chars.add(randomCharFrom(LOWER));
        }


        if(policy.isDigit()){
            chars.add(randomCharFrom(DIGITS));
        }


        if(policy.isSpecial()){
            chars.add(randomCharFrom(policy.getSpecialChars()));
        }

        String allChars  = buildAllChars(policy);

        // 补齐剩余长度
        while (chars.size() < policy.getLength()) {
            chars.add(randomCharFrom(allChars));
        }

        // 打乱顺序
        Collections.shuffle(chars, RANDOM);

        StringBuilder password = new StringBuilder(policy.getLength());
        for (Character c : chars) {
            password.append(c);
        }
        return password.toString();
    }


    /**
     * 根据策略构造字符池
     */
    private static String buildAllChars(PasswordPolicy policy) {

        StringBuilder chars = new StringBuilder();

        if (policy.isUppercase()) {
            chars.append(UPPER);
        }

        if (policy.isLowercase()) {
            chars.append(LOWER);
        }


        if (policy.isDigit()) {
            chars.append(DIGITS);
        }


        if (policy.isSpecial()) {
            chars.append(policy.getSpecialChars());
        }

        return chars.toString();

    }

    private static char randomCharFrom(String chars) {
        return chars.charAt(RANDOM.nextInt(chars.length()));
    }

}
