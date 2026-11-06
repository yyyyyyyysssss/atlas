package com.atlas.auth.config.security.cas;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;


@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        property = "@class"
)
public record CasProxyGrantingTicketContext(
        String pgt,
        Long userId,
        String service,
        String pgtUrl,
        List<String> proxies
) {
}
