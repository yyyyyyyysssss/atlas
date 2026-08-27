package com.atlas.auth.config.security.mfa;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

public class RedisMfaTicketRepository implements MfaTicketRepository{

    private static final String TICKET_KEY_PREFIX = "security:mfa:ticket:";

    private final RedisTemplate<String,Object> redisTemplate;

    public RedisMfaTicketRepository(RedisTemplate<String,Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(MfaChallenge mfaChallenge){
        String key = redisKey(mfaChallenge.getTicket());
        redisTemplate.opsForValue().set(key, mfaChallenge, Duration.ofMinutes(5));
    }

    @Override
    public MfaChallenge load(String ticket) {
        String key = redisKey(ticket);
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof MfaChallenge mfaChallenge) {
            return mfaChallenge;
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
