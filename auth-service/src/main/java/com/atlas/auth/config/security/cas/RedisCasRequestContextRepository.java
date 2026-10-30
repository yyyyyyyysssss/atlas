package com.atlas.auth.config.security.cas;

import com.atlas.common.redis.utils.RedisHelper;

import java.time.Duration;
import java.util.UUID;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/30 9:55
 */
public class RedisCasRequestContextRepository implements CasRequestContextRepository{

    private static final String KEY_PREFIX = "cas:request:";

    /**
     * 请求上下文有效期
     */
    private static final Duration CONTEXT_TTL = Duration.ofMinutes(5);

    private final RedisHelper redisHelper;

    public RedisCasRequestContextRepository(RedisHelper redisHelper){
        this.redisHelper = redisHelper;
    }

    @Override
    public String save(CasRequestContext context) {
        String casRequestId = UUID.randomUUID().toString().replace("-", "");
        String key = KEY_PREFIX + casRequestId;
        redisHelper.setValue(key, context, CONTEXT_TTL);
        return casRequestId;
    }

    @Override
    public CasRequestContext consume(String casRequestId) {
        if (casRequestId == null || casRequestId.isBlank()) {
            return null;
        }
        String key = KEY_PREFIX + casRequestId;
        return redisHelper.getValueAndDelete(key, CasRequestContext.class);
    }
}
