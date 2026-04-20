package com.atlas.common.redis.queue;

import com.atlas.common.core.queue.DistributedDelayQueue;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedissonDistributedDelayQueue implements DistributedDelayQueue, SmartLifecycle {

    private final RedissonClient redissonClient;

    /**
     * 实例缓存：针对同一 Topic 保证单机内只创建一个 DelayedQueue 包装对象
     * 避免高频调用 addJob 时产生大量的监听器和定时任务对象
     */
    private final Map<String, RDelayedQueue<Object>> queueCache = new ConcurrentHashMap<>();

    private volatile boolean isRunning = false;

    public RedissonDistributedDelayQueue(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void start() {
        log.info("[RedissonDelayQueue] Starting delay queue manager component.");
        this.isRunning = true;
    }

    @Override
    public <T> void sendMessage(String topicName, T data, long delay, TimeUnit unit) {
        if (!isRunning) {
            log.warn("[RedissonDelayQueue] Rejected adding job because component is stopped. topic={}", topicName);
            throw new IllegalStateException("RedissonDistributedDelayQueue is not running");
        }
        Assert.hasText(topicName, "Topic name must not be empty");
        Assert.notNull(data, "Delay job data must not be null");
        Assert.notNull(unit, "TimeUnit must not be null");
        try {
            // 获取（或创建）延迟队列实例
            RDelayedQueue<Object> delayedQueue = queueCache.computeIfAbsent(topicName, name -> {
                log.info("[RedissonDelayQueue] Initializing new delay queue instance. topic={}", name);
                // 使用 Deque 以获得更强的扩展性（支持双端操作）
                RBlockingDeque<Object> blockingDeque = redissonClient.getBlockingDeque(name);
                return redissonClient.getDelayedQueue(blockingDeque);
            });
            delayedQueue.offer(data, delay, unit);
            if (log.isDebugEnabled()) {
                log.debug("[RedissonDelayQueue] Adding job to queue. topic={}, delay={}, unit={}, data={}",
                        topicName, delay, unit, data);
            }
        } catch (Exception e) {
            log.error("[RedissonDelayQueue] Failed to add job. topic={}, delay={}, unit={}. Error: {}",
                    topicName, delay, unit, e.getMessage(), e);
            throw new RuntimeException("Push delay job failed", e);
        }
    }

    @Override
    public void stop() {
        log.info("[RedissonDelayQueue] Shutting down. Active queues={}...",queueCache.size());
        this.isRunning = false;
        queueCache.forEach((name, queue) -> {
            try {
                queue.destroy();
                log.info("[RedissonDelayQueue] Successfully destroyed queue: {}", name);
            } catch (Exception e) {
                log.warn("[RedissonDelayQueue] Error occurred while destroying queue: {}. Reason: {}",
                        name, e.getMessage());
            }
        });
        queueCache.clear();
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }
}
