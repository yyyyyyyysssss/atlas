package com.atlas.common.security.service;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.crypto.random.SecureRandomUtils;
import com.atlas.common.redis.utils.RedisHelper;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/14 14:55
 */
public class RedisTicketService implements TicketService{

    private final RedisHelper redisHelper;

    private static final String PREFIX = "security:operation-ticket:";

    public RedisTicketService(RedisHelper redisHelper){
        this.redisHelper = redisHelper;
    }

    @Override
    public String create(Object value, Duration timeout) {
        String ticket = "AT-" + SecureRandomUtils.generate();
        redisHelper.setValue(PREFIX + ticket, value, timeout);
        return ticket;
    }

    @Override
    public <T> T get(String ticket, Class<T> type) {
        String key = buildKey(ticket);
        return redisHelper.getValue(key, type);
    }

    @Override
    public <T> T consume(String ticket, Class<T> type) {
        String key = buildKey(ticket);
        T value = redisHelper.getValueAndDelete(key, type);
        if (value == null) {
            throw new BusinessException("ticket已失效");
        }
        return value;
    }

    private String buildKey(String ticket) {
        if (!StringUtils.hasText(ticket) || !ticket.startsWith("AT-")) {
            throw new BusinessException("无效的ticket");
        }
        return PREFIX + ticket;
    }
}
