package com.atlas.common.redis.sequence;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;

/**
 * @Description
 * @Author ys
 * @Date 2025/12/10 15:14
 */
public class SequenceNumberPart implements SequencePart{

    private final RedisTemplate<String, Object> redisTemplate;

    private final int length;

    private final String keyPrefix;

    private final long sequenceMax;

    private static final RedisScript<Long> SEQUENCE_SCRIPT = RedisScript.of(
            "local current = redis.call('INCR', KEYS[1]) " +
                    "local max = tonumber(ARGV[1]) " +
                    "if current >= max then " +
                    "   redis.call('SET', KEYS[1], 0) " +
                    "   current = 0 " +
                    "end " +
                    "return current", Long.class);

    public SequenceNumberPart(RedisTemplate<String, Object> redisTemplate, int length,String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.length = length;
        this.keyPrefix = keyPrefix;
        this.sequenceMax = (long) Math.pow(10, length);
    }

    @Override
    public String generate(String bizContent) {
        String seqKey = keyPrefix + ":" + bizContent;
        // 执行 Lua 脚本，保证递增、超出最大值时重置
        Long sequence = redisTemplate.execute(
                SEQUENCE_SCRIPT,
                Collections.singletonList(seqKey),
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
