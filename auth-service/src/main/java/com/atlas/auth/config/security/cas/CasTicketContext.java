package com.atlas.auth.config.security.cas;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;


@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        property = "@class"
)
public record CasTicketContext(
        Long userId,

        List<String> proxies
) {
}
