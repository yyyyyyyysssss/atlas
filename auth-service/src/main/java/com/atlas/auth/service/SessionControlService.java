package com.atlas.auth.service;

import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.security.constant.SecurityConstant;
import com.atlas.security.properties.SecurityProperties;
import com.atlas.security.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/25 11:40
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionControlService {

    private final TokenService tokenService;

    private final SecurityProperties securityProperties;

    private final RedisHelper redisHelper;

    public void kickOutExcessiveSessions(Long userId){
        Integer maxSessions = securityProperties.getCoexistToken();
        if (maxSessions <= 0){
            return;
        }
        String limitKey = SecurityConstant.USER_TOKEN_LIMIT + userId;
        // 先清理掉 ZSet 中已经过期的无效数据
        redisHelper.removeZSetByScore(limitKey, 0, System.currentTimeMillis());
        // 获取当前剩余的有效会话
        List<ZSetOperations.TypedTuple<String>> sessions = redisHelper.rangeScoreZSet(limitKey, 0, -1, String.class);
        // 计算需要预留的位置。如果要存入 1 个新 Token，那么现有的数量不能超过 (limit - 1)
        int reserveQty = securityProperties.getCoexistToken() - 1;
        if(CollectionUtils.isEmpty(sessions) || sessions.size() <= reserveQty){
            return;
        }
        int kickCount = sessions.size() - reserveQty;
        for (int i = 0; i < kickCount; i++) {
            ZSetOperations.TypedTuple<String> stringTypedTuple = sessions.get(i);
            String oldTokenId = stringTypedTuple.getValue();
            Double expScore = stringTypedTuple.getScore();
            // 执行注销
            tokenService.revoke(oldTokenId, expScore.longValue());
            // 从 ZSet 移除，防止脏数据
            removeSession(userId,oldTokenId);
        }
    }

    public void registerSession(Long userId, String tokenId, long expiration) {
        String limitKey = SecurityConstant.USER_TOKEN_LIMIT + userId;
        redisHelper.addZSet(limitKey, tokenId, (double) expiration);
    }

    public void removeSession(Long userId, String tokenId){
        String limitKey = SecurityConstant.USER_TOKEN_LIMIT + userId;
        redisHelper.removeZSet(limitKey,tokenId);
    }

}
