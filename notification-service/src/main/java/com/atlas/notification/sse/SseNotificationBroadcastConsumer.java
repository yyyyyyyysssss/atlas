package com.atlas.notification.sse;

import com.atlas.common.redis.queue.AbstractBroadcastStreamConsumer;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/20 13:36
 */
@Slf4j
@Component
public class SseNotificationBroadcastConsumer extends AbstractBroadcastStreamConsumer<NotificationStreamMessage> {

    public static final String TOPIC_NAME = "topic:notification:sse";

    private final SseSessionManager sseSessionManager;

    protected SseNotificationBroadcastConsumer(RedissonClient redissonClient,SseSessionManager sseSessionManager) {
        super(redissonClient, TOPIC_NAME);
        this.sseSessionManager = sseSessionManager;
    }

    @Override
    protected void onMessage(NotificationStreamMessage payload) {
        try {
            // 解析出通知需要的参数
            Long userId = payload.getUserId();
            String eventCode = payload.getEventCode();
            Object content = payload.getData();
            // 调用你的 SseSessionManager 进行本地推送
            if (userId == null) {
                sseSessionManager.broadcast(eventCode, content);
            } else {
                sseSessionManager.sendToUser(userId, eventCode, content);
            }
        } catch (Exception e) {
            log.error("解析分布式通知消息失败", e);
        }
    }
}
