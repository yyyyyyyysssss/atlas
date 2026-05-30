package com.atlas.auth.config.security.mfa;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

public class RedisMfaTicketRepository implements MfaTicketRepository{

    private static final String TICKET_KEY_PREFIX = "mfa:ticket:";

    private final RedisTemplate<String,Object> redisTemplate;

    public RedisMfaTicketRepository(RedisTemplate<String,Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String ticket, MfaTicketContext mfaTicketContext, Duration timeout) {
        String key = redisKey(ticket);
        // 存入 Redis 并直接设置过期时间
        redisTemplate.opsForValue().set(key, mfaTicketContext, timeout);
    }

    @Override
    public MfaTicketContext load(String ticket) {
        String key = redisKey(ticket);
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof MfaTicketContext mfaTicketContext) {
            return mfaTicketContext;
        }
        return null;
    }

    @Override
    public void remove(String ticket) {
        String key = redisKey(ticket);
        redisTemplate.delete(key);
    }

    private String redisKey(String ticket) {
        return TICKET_KEY_PREFIX + ticket;
    }
}
