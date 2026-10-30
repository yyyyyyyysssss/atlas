package com.atlas.auth.config.security.cas;

import com.atlas.auth.config.exception.AuthErrorCode;
import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.common.crypto.digest.DigestUtils;
import com.atlas.common.crypto.random.SecureRandomUtils;
import com.atlas.common.redis.utils.RedisHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/28 11:24
 */
@Component
@Slf4j
public class CasService {


    @Resource
    private RedisHelper redisHelper;

    private static final String KEY_PREFIX = "cas:ticket:";

    /**
     * CAS Service Ticket 有效期。
     */
    private static final Duration TICKET_TTL = Duration.ofSeconds(300);


    // 签发 CAS Service Ticket。
    public String issueServiceTicket(String service, Long userId){
        if (service == null || service.isBlank() || userId == null) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, service);
        }
        String ticket = "ST-" + SecureRandomUtils.generate(32);
        String key = buildKey(ticket, service);
        boolean success = redisHelper.setIfAbsent(key, userId, TICKET_TTL);
        if (!success) {
            throw new AuthServiceException("CAS Service Ticket already exists");
        }
        return ticket;
    }

    // 消费 CAS Service Ticket。
    public Long consumeServiceTicket(String ticket, String service) {
        if (ticket == null || ticket.isBlank() || service == null || service.isBlank()) {
            throw new CasAuthenticationException(AuthErrorCode.CAS_INVALID_REQUEST, service);
        }
        String key = buildKey(ticket, service);
        return redisHelper.getValueAndDelete(key, Long.class);
    }

    // 创建 CAS Service Ticket 验证响应
    public String createServiceResponse(String service, String ticket, String relayState) {

        return null;
    }

    /**
     * 创建 CAS Service Ticket 验证失败响应
     *
     * @param errorCode CAS 协议错误代码
     * @param description 错误描述
     */
    public String createServiceFailureResponse(String errorCode, String description) {
        return null;
    }



    private String buildKey(String ticket, String service) {
        return KEY_PREFIX + ticket + ":" + DigestUtils.sha256(service);
    }

}
