package com.atlas.common.security.service;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.crypto.random.SecureRandomUtils;
import com.atlas.common.redis.utils.RedisHelper;

import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/14 14:55
 */
public class RedisTicketService implements TicketService{

    private final RedisHelper redisHelper;

    private static final String PREFIX = "security:ticket:";

    public RedisTicketService(RedisHelper redisHelper){
        this.redisHelper = redisHelper;
    }

    @Override
    public String create(Object value, Duration timeout) {
        String ticket = SecureRandomUtils.generate();
        redisHelper.setValue(PREFIX + ticket, value, timeout);
        return ticket;
    }

    @Override
    public <T> T get(String ticket, Class<T> type) {
        return redisHelper.getValue(PREFIX + ticket, type);
    }

    @Override
    public <T> T consume(String ticket, Class<T> type) {
        String key = PREFIX + ticket;
        T value = redisHelper.getValue(key, type);
        if (value == null) {
            throw new BusinessException("ticket已失效");
        }
        redisHelper.delete(key);
        return value;
    }
}
