package com.atlas.common.redis.queue;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedissonDistributedDelayQueue implements DistributedDelayQueue, DisposableBean {

    private final RedissonClient redissonClient;

    /**
     * 实例缓存：针对同一 Topic 保证单机内只创建一个 DelayedQueue 包装对象
     * 避免高频调用 addJob 时产生大量的监听器和定时任务对象
     */
    private final Map<String, RDelayedQueue<?>> queueCache = new ConcurrentHashMap<>();

    public RedissonDistributedDelayQueue(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> void addJob(String topicName, T data, long delay, TimeUnit unit) {
        Assert.hasText(topicName, "Topic name must not be empty");
        Assert.notNull(data, "Delay job data must not be null");
        Assert.notNull(unit, "TimeUnit must not be null");
        try {
            // 获取（或创建）延迟队列实例
            RDelayedQueue<T> delayedQueue = (RDelayedQueue<T>) queueCache.computeIfAbsent(topicName, name -> {
                log.info("初始化分布式延迟队列实例: {}", name);
                // 使用 Deque 以获得更强的扩展性（支持双端操作）
                RBlockingDeque<Object> blockingDeque = redissonClient.getBlockingDeque(name);
                return redissonClient.getDelayedQueue(blockingDeque);
            });
            log.info("添加延迟任务: queue={}, delay={}, unit={}", topicName, delay, unit);
            delayedQueue.offer(data, delay, unit);
        } catch (Exception e) {
            log.error("添加延迟任务失败: queue={}", topicName, e);
            throw new RuntimeException("Push delay job failed", e);
        }
    }

    @Override
    public void destroy() {
        log.info("正在关闭 Redisson 分布式延迟队列资源...");
        queueCache.forEach((name, queue) -> {
            try {
                queue.destroy();
            } catch (Exception e) {
                log.warn("销毁队列 {} 失败", name);
            }
        });
        queueCache.clear();
    }
}
