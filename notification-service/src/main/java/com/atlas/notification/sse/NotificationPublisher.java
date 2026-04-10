package com.atlas.notification.sse;


import com.atlas.common.core.api.notification.enums.NotificationEventEnum;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher implements SmartLifecycle {


    // 自动注入所有实现 NotificationSubscriber 接口的 Bean
    private final List<NotificationSubscriber> subscribers;

    // 消息缓冲队列，容量根据内存情况调整
    private final BlockingQueue<NotificationTask> taskQueue = new LinkedBlockingQueue<>(10000);

    private volatile boolean isRunning = false;

    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    @Override
    public void start() {
        if (isRunning){
            return; // 防止重复启动
        }
        isRunning = true;
        Thread.startVirtualThread(() -> {
            log.info("Notification Publisher 消费线程（虚拟线程）已启动");
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // 如果已经停止运行 且 队列空了，说明优雅停机完成，正常退出
                    if (!isRunning && taskQueue.isEmpty()) {
                        log.info("检测到优雅停机信号且队列已清空，准备退出...");
                        break;
                    }
                    try {
                        NotificationTask task = taskQueue.poll(500, TimeUnit.MILLISECONDS);
                        if (task == null) {
                            continue;
                        }
                        // 分发给所有订阅者（如 SseSessionManager）
                        for (NotificationSubscriber subscriber : subscribers) {
                            try {
                                subscriber.onMessage(task.userId(), task.eventName(), task.data());
                            } catch (Exception e) {
                                log.error("订阅者 [{}] 处理失败", subscriber.getClass().getSimpleName(), e);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } finally {
                shutdownLatch.countDown();
                log.info("Notification Publisher 消费线程已彻底结束");
            }

        });
    }

    @Override
    public void stop() {
        isRunning = false;
        // 异步监控队列清空情况
        Thread.startVirtualThread(() -> {
            try {
                log.info("正在等待消费线程处理剩余任务...");
                // 优雅地等待信号，最多等 15 秒
                if (!shutdownLatch.await(15, TimeUnit.SECONDS)) {
                    log.warn("停机等待超时，部分存量消息可能未送达");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    /**
     * 业务方调用此方法发布通知
     */
    public void publish(Long userId, NotificationEventEnum eventEnum, Object data) {
        if (!isRunning) {
            log.warn("系统正在关闭，拒绝发送通知: [event={}, userId={}]", eventEnum.getCode(), userId);
            return;
        }
        boolean offered = taskQueue.offer(new NotificationTask(userId, eventEnum, data));
        if (!offered) {
            log.error("通知队列已满({}/10000)，丢弃消息: {}", taskQueue.size(), eventEnum.getCode());
        }
    }

    private record NotificationTask(Long userId, NotificationEventEnum eventName, Object data) {
    }

}
