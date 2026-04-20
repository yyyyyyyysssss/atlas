package com.atlas.common.redis.queue;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamMessageId;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/20 10:51
 */
@Slf4j
public abstract class AbstractStreamConsumer<T> implements SmartLifecycle {


    @Getter
    protected final RedissonClient redissonClient;
    @Getter
    private final String topicName;
    @Getter
    private final String groupName;
    private final String consumerName;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // 运行状态标识
    private volatile boolean isRunning = false;

    protected AbstractStreamConsumer(RedissonClient redissonClient, String topicName, String groupName) {
        this.redissonClient = redissonClient;
        this.topicName = topicName;
        this.groupName = groupName;
        this.consumerName = "cns-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 业务逻辑
     */
    protected abstract void onMessage(T data);

    @Override
    public void start() {
        log.info("[StreamConsumer] Starting. topic={}, group={}, consumer={}", topicName, groupName, consumerName);
        this.isRunning = true;
        // 使用虚拟线程启动监听死循环
        Thread.ofVirtual().name("v-stream-worker-" + topicName).start(this::consumeLoop);
    }

    private void consumeLoop() {
        RStream<String, Object> stream = redissonClient.getStream(topicName);
        // 初始化消费组
        try {
            stream.createGroup(StreamCreateGroupArgs.name(groupName).makeStream());
        } catch (Exception ignore) {
            // 组已存在通常无需处理
        }
        while (isRunning) {
            try {
                // 阻塞拉取消息
                Map<StreamMessageId, Map<String, Object>> messages = stream.readGroup(
                        groupName, consumerName,
                        StreamReadGroupArgs.neverDelivered().count(5).timeout(Duration.ofSeconds(10))
                );
                // 判空，因为 timeout 到期且无新消息时会返回空集合
                if (messages == null || messages.isEmpty()) {
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug("[StreamConsumer] Pulled messages count={}", messages.size());
                }
                messages.forEach((id, payload) -> executor.submit(() -> {
                    long taskStartTime = System.currentTimeMillis();
                    try {
                        @SuppressWarnings("unchecked")
                        T data = (T) payload.get("payload");

                        // 核心业务回调
                        onMessage(data);
                        // 消息 ACK
                        stream.ack(groupName, id);
                        log.info("[StreamConsumer] Processed success. topic={}, id={}, cost={}ms",
                                topicName, id, (System.currentTimeMillis() - taskStartTime));
                    } catch (Exception e) {
                        log.error("[StreamConsumer] Processed failed. topic={}, id={}, data={}, error={}",
                                topicName, id, payload, e.getMessage(), e);
                    }
                }));
            } catch (Exception e) {
                if (!isRunning) break; // 正常停止时不打印异常
                log.error("[StreamConsumer] Listening loop error. topic={}, error={}", topicName, e.getMessage(), e);
                log.error("[StreamConsumer] 循环错误. topic={}", topicName, e);
                try {
                    // 异常退避，防止高速报错刷屏
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public void stop() {
        log.info("[StreamConsumer] Shutting down. topic={}", topicName);
        this.isRunning = false;
        // 优雅关闭执行器
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("[StreamConsumer] Executor forced shutdown topic={}", topicName);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("[StreamConsumer] stopped. topic={}", topicName);
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }
}
