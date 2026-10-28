package com.atlas.auth.config.security.cas;

import com.atlas.common.crypto.random.SecureRandomUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/28 11:24
 */
@Component
@Slf4j
public class CasService {

    // 生成 Service Ticket
    public String generateServiceTicket(Long userId, String service){

        return "ST-" + SecureRandomUtils.generate(32);
    }

    /**
     * 创建 CAS Service Ticket 验证响应
     *
     * @param ticketContext Ticket 验证上下文
     * @param service 业务系统地址
     * @return CAS XML 响应
     */
    public String createServiceResponse(CasTicketContext ticketContext, String service) {

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

}
