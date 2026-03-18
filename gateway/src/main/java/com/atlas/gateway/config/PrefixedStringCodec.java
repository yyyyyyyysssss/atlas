package com.atlas.gateway.config;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

import java.nio.ByteBuffer;

/**
 * @Description
 * @Author ys
 * @Date 2026/3/18 17:07
 */
public class PrefixedStringCodec implements RedisCodec<String, byte[]> {

    private final String prefix;
    private final StringCodec stringCodec = StringCodec.UTF8;
    private final ByteArrayCodec byteCodec = ByteArrayCodec.INSTANCE;

    public PrefixedStringCodec(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String decodeKey(ByteBuffer byteBuffer) {
        String fullKey = stringCodec.decodeKey(byteBuffer);
        return fullKey.startsWith(prefix) ? fullKey.substring(prefix.length()) : fullKey;
    }

    @Override
    public byte[] decodeValue(ByteBuffer byteBuffer) {
        return byteCodec.decodeValue(byteBuffer);
    }

    @Override
    public ByteBuffer encodeKey(String s) {
        return stringCodec.encodeKey(prefix + s);
    }

    @Override
    public ByteBuffer encodeValue(byte[] bytes) {
        return byteCodec.encodeValue(bytes);
    }
}
