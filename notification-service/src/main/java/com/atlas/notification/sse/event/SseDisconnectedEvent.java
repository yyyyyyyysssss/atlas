package com.atlas.notification.sse.event;

public record SseDisconnectedEvent(Long userId, String terminal, String reason) {
}
