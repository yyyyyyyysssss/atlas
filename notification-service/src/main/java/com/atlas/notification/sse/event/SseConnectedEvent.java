package com.atlas.notification.sse.event;

public record SseConnectedEvent(Long userId, String terminal) {
}
