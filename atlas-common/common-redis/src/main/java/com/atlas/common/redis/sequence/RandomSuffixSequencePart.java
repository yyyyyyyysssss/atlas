package com.atlas.common.redis.sequence;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @Description
 * @Author ys
 * @Date 2025/12/10 15:16
 */
public class RandomSuffixSequencePart implements SequencePart{

    private final int bit;

    public RandomSuffixSequencePart(int bit){
        if (bit <= 0) {
            throw new IllegalArgumentException("随机数位数必须大于0");
        }
        this.bit = bit;
    }

    @Override
    public String generate(String bizContent) {
        int bound = (int) Math.pow(10, bit);
        // 获取一位的随机数
        int randomNumber = ThreadLocalRandom.current().nextInt(bound);
        return String.format("%0" + bit + "d", randomNumber);
    }
}
