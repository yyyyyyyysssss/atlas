package com.atlas.auth.config.security.cas;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public enum CasTicketType {

    SERVICE("ST-", Duration.ofSeconds(300)),

    PROXY("PT-", Duration.ofSeconds(300));

    private final String prefix;

    private final Duration ttl;


}
