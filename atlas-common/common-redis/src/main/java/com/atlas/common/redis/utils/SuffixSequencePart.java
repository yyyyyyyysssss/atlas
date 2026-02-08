package com.atlas.common.redis.utils;

import java.util.Random;

/**
 * @Description
 * @Author ys
 * @Date 2025/12/10 15:16
 */
public class SuffixSequencePart implements SequencePart{

    @Override
    public String generate(String bizContent) {
        // 获取一位的随机数
        int randomDigit = new Random().nextInt(10);
        return String.valueOf(randomDigit);
    }
}
