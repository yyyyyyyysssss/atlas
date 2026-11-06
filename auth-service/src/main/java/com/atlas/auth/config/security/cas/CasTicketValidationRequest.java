package com.atlas.auth.config.security.cas;

public record CasTicketValidationRequest(
        String ticket,
        String service,
        String pgtUrl,
        CasProtocolVersion version,
        CasTicketValidationType type

) {

    public static CasTicketValidationRequest service(String ticket, String service, String pgtUrl, CasProtocolVersion version) {

        return new CasTicketValidationRequest(ticket, service, pgtUrl, version, CasTicketValidationType.SERVICE);
    }

    public static CasTicketValidationRequest proxy(String ticket, String service, CasProtocolVersion version) {

        return new CasTicketValidationRequest(ticket, service, null, version, CasTicketValidationType.PROXY);
    }

    public enum CasTicketValidationType {

        // 普通 Service Ticket 验证
        SERVICE,

        // 代理票据验证
        PROXY
    }

}
