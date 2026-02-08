package com.atlas.common.redis.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;

/**
 * @Description
 * @Author ys
 * @Date 2025/12/10 15:14
 */
public class SequenceNumberPart implements SequencePart{

    private RedisTemplate<String, Object> redisTemplate;

    private int length;

    public SequenceNumberPart(RedisTemplate<String, Object> redisTemplate, int length) {
        this.redisTemplate = redisTemplate;
        this.length = length;
    }

    @Override
    public String generate(String bizContent) {
        // 计算最大序列值
        int sequenceMax = (int) Math.pow(10, length) - 1;
        // 执行 Lua 脚本，保证递增、超出最大值时重置
        Long sequence = redisTemplate.execute(
                RedisScript.of(getLuaScript(), Long.class),
                Collections.singletonList(bizContent),
                sequenceMax
        );
        return String.format("%0" + length + "d", sequence);
    }

    private String getLuaScript() {
        return "local sequence = redis.call('INCR', KEYS[1]) " +
                "local sequenceMax = tonumber(ARGV[1]) " +
                "if sequence > sequenceMax then " +
                "   redis.call('SET', KEYS[1], 0) " +
                "   sequence = 0 " +
                "end " +
                "return sequence";
    }
}
