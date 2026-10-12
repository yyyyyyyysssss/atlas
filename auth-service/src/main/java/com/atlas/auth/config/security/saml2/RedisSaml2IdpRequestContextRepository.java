package com.atlas.auth.config.security.saml2;

import com.atlas.common.crypto.digest.DigestUtils;
import com.atlas.common.redis.utils.RedisHelper;

import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/12 10:09
 */
public final class RedisSaml2IdpRequestContextRepository implements Saml2IdpRequestContextRepository{

    private final RedisHelper redisHelper;

    private static final String SAML_REQUEST_REPLAY_KEY = "saml2:idp:replay:";

    private static final String SAML_REQUEST_CONTEXT_KEY = "saml2:idp:context:";

    private static final Duration REPLAY_TTL = Duration.ofMinutes(5);

    private static final Duration CONTEXT_TTL = Duration.ofMinutes(5);

    public RedisSaml2IdpRequestContextRepository(RedisHelper redisHelper){
        this.redisHelper = redisHelper;
    }

    @Override
    public void save(String issuer, Saml2IdpRequestContext context) {
        String key = buildKey(SAML_REQUEST_CONTEXT_KEY, issuer, context.getRequestId());
        redisHelper.setValue(key, context, REPLAY_TTL);
    }

    @Override
    public Saml2IdpRequestContext find(String issuer, String requestId) {
        String key = buildKey(SAML_REQUEST_CONTEXT_KEY, issuer, requestId);
        return redisHelper.getValue(key, Saml2IdpRequestContext.class);
    }

    @Override
    public void remove(String issuer, String requestId) {
        String key = buildKey(SAML_REQUEST_CONTEXT_KEY, issuer, requestId);
        redisHelper.delete(key);
    }

    @Override
    public boolean markAsSeen(String issuer, String requestId) {
        String key = buildKey(SAML_REQUEST_REPLAY_KEY, issuer, requestId);
        return redisHelper.setIfAbsent(key, "1", CONTEXT_TTL);
    }

    private String buildKey(String prefix, String issuer, String requestId) {
        return prefix
                + DigestUtils.sha256(issuer)
                + ":"
                + requestId;
    }
}
