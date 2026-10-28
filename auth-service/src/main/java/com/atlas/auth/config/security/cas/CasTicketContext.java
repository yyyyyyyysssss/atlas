package com.atlas.auth.config.security.cas;

public record CasTicketContext(

        // CAS Service Ticket
        String ticket,

        // 业务系统回调地址
        String service,

        // 登录后是否要求重新认证
        boolean renew,

        // 是否为无交互登录请求
        boolean gateway,

        // CAS 登录请求携带的 RelayState
        String relayState
) {
}
