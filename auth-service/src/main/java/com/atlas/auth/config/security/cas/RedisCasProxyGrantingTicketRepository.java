package com.atlas.auth.config.security.cas;

import com.atlas.common.crypto.digest.DigestUtils;
import com.atlas.common.redis.utils.RedisHelper;
import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/6 11:07
 */
@Slf4j
@RequiredArgsConstructor
@Repository
public class RedisCasProxyGrantingTicketRepository implements CasProxyGrantingTicketRepository{

    private static final String KEY_PREFIX = "cas:ticket:pgt:";

    private final RedisHelper redisHelper;

    /**
     * PGT 有效期：8 小时
     */
    private static final Duration PGT_TTL = Duration.ofHours(8);


    @Override
    public void save(CasProxyGrantingTicketContext context) {
        if (context == null || !StringUtils.hasText(context.pgt())) {
            throw new IllegalArgumentException("PGT context or ticket cannot be empty");
        }
        String key = buildKey(context.pgt());
        redisHelper.setValue(key, context, PGT_TTL);
    }

    @Override
    public CasProxyGrantingTicketContext findByTicket(String pgt) {
        if (!StringUtils.hasText(pgt)) {
            return null;
        }
        String key = buildKey(pgt);

        return redisHelper.getValue(key, CasProxyGrantingTicketContext.class);
    }

    @Override
    public void deleteByTicket(String pgt) {
        if (!StringUtils.hasText(pgt)) {
            return;
        }
        String key = buildKey(pgt);
        redisHelper.delete(key);
    }

    @Override
    public boolean exists(String pgt) {
        if (!StringUtils.hasText(pgt)) {
            return false;
        }
        String key = buildKey(pgt);
        return redisHelper.hasKey(key);
    }


    private String buildKey(String ticket) {
        return KEY_PREFIX + DigestUtils.sha256(ticket);
    }
}
