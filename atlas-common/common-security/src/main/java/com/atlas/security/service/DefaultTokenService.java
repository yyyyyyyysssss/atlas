package com.atlas.security.service;

import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.security.constant.SecurityConstant;
import com.atlas.security.enums.TokenType;
import com.atlas.security.model.PayloadInfo;
import com.atlas.security.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 11:02
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultTokenService implements TokenService {

    private final JwtUtils jwtUtils;

    private final RedisHelper redisHelper;

    @Override
    public PayloadInfo verify(String token) {
        if(!jwtUtils.verifier(token)){
            log.warn("Token signature invalid or expired");
            return null;
        }
        PayloadInfo payloadInfo = jwtUtils.extractPayloadInfo(token);
        if(payloadInfo == null || !TokenType.ACCESS_TOKEN.equals(payloadInfo.getTokenType())){
            log.warn("Invalid token type: {}", payloadInfo != null ? payloadInfo.getTokenType() : "null");
            return null;
        }
        String tokenId = payloadInfo.getId();
        //黑名单
        if(redisHelper.hasKey(SecurityConstant.TOKEN_BLACKLIST + tokenId)){
            log.warn("Token is in blacklist, ID: {}", tokenId);
            return null;
        }
        return payloadInfo;
    }

}
