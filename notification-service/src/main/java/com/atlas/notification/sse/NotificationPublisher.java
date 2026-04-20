package com.atlas.notification.sse;


import com.atlas.common.core.api.notification.enums.NotificationEventEnum;
import com.atlas.common.redis.queue.RedissonStreamProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher{

    private final RedissonStreamProducer streamProducer;


    /**
     * 业务方调用此方法发布通知
     */
    public void publish(Long userId, NotificationEventEnum eventEnum, Object data) {
        NotificationStreamMessage payload = NotificationStreamMessage.of(
                userId,
                eventEnum.getCode(),
                data
        );
        streamProducer.sendMessage(SseNotificationBroadcastConsumer.TOPIC_NAME, payload);
    }

}
