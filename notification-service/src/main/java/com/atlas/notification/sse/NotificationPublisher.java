package com.atlas.notification.sse;


import com.atlas.common.core.api.notification.enums.NotificationEventEnum;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {


    // 自动注入所有实现 NotificationSubscriber 接口的 Bean
    private final List<NotificationSubscriber> subscribers;

    // 消息缓冲队列，容量根据内存情况调整
    private final BlockingQueue<NotificationTask> taskQueue = new LinkedBlockingQueue<>(10000);

    @PostConstruct
    public void init() {
        Thread.startVirtualThread(() -> {
            log.info("Notification Consumer (Virtual Thread) Started.");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    NotificationTask task = taskQueue.take();
                    // 分发给所有订阅者（如 SseSessionManager）
                    for (NotificationSubscriber subscriber : subscribers) {
                        try {
                            subscriber.onMessage(task.userId(), task.eventName(), task.data());
                        } catch (Exception e) {
                            log.error("Subscriber [{}] process failed", subscriber.getClass().getSimpleName(), e);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /**
     * 业务方调用此方法发布通知
     */
    public void publish(Long userId, NotificationEventEnum eventEnum, Object data) {
        boolean offered = taskQueue.offer(new NotificationTask(userId, eventEnum, data));
        if (!offered) {
            log.warn("Notification queue is full! Dropping message: {}", eventEnum.getCode());
        }
    }

    public void publish(NotificationEventEnum eventEnum, Object data) {
        publish(null, eventEnum, data);
    }

    private record NotificationTask(Long userId, NotificationEventEnum eventName, Object data) {}

}
