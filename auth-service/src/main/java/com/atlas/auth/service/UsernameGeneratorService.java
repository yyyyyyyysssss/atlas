package com.atlas.auth.service;

import com.atlas.common.core.idwork.IdGen;
import org.springframework.stereotype.Service;

import java.util.function.Predicate;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/18 10:23
 */
@Service
public class UsernameGeneratorService {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int BASE = ALPHABET.length();
    private static final int TARGET_LENGTH = 8;  // 用户名主体长度
    private static final String PREFIX = "u_";

    public String generateUniqueUsername(Predicate<String> existsChecker) {
        String username;
        int attempt = 0;

        do {
            if (attempt > 10) {
                throw new RuntimeException("Failed to generate unique username after 10 attempts");
            }
            username = PREFIX + generateFromId();
            attempt++;
        } while (existsChecker.test(username)); // 回调外部的校验逻辑

        return username;
    }

    /**
     * 基于雪花ID / 全局唯一ID生成 base36 用户名
     */
    private String generateFromId() {
        long id = IdGen.genId(); // 全局唯一ID
        long limitedId = id % 2821109907456L; // 36^8，保证长度不超过8位

        StringBuilder sb = new StringBuilder();
        long tempId = limitedId;

        // 核心转换逻辑
        while (tempId > 0) {
            sb.append(ALPHABET.charAt((int) (tempId % BASE)));
            tempId /= BASE;
        }

        // 不足8位补齐
        while (sb.length() < TARGET_LENGTH) {
            sb.append(ALPHABET.charAt(0));
        }

        // 反转字符串并返回
        return sb.reverse().toString();
    }

}
