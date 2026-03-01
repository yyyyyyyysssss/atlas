package com.atlas.common.redis.autoconfigure;

import org.springframework.data.redis.serializer.StringRedisSerializer;

public class PrefixStringSerializer extends StringRedisSerializer {

    private final String prefix;

    public PrefixStringSerializer(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public byte[] serialize(String value) {
        // 在写入 Redis 前拼接前缀
        String prefixedKey = prefix + ":" + value;
        return super.serialize(prefixedKey);
    }

}
