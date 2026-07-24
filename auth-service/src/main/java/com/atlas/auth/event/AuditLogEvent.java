package com.atlas.auth.event;

public record AuditLogEvent(
        Long userId,

        String summary,

        String target
) {
}
