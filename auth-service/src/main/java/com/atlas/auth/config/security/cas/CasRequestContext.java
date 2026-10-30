package com.atlas.auth.config.security.cas;

public record CasRequestContext(
        String service,

        boolean renew,

        boolean gateway,

        String relayState
) {
}
