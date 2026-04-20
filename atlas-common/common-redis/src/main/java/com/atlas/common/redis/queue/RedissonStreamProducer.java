package com.atlas.common.redis.queue;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamMessageId;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/20 10:48
 */
@Slf4j
public class RedissonStreamProducer implements SmartLifecycle {

    private final RedissonClient redissonClient;

    private volatile boolean isRunning = false;

    public RedissonStreamProducer(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }


    @Override
    public void start() {
        log.info("[StreamProducer] Starting...");
        this.isRunning = true;
    }

    /**
     * 发送泛型消息
     *
     * @param topicName 队列名称
     * @param data      业务对象
     */
    public <T> StreamMessageId sendMessage(String topicName, T data) {
        if (!isRunning) {
            log.warn("[StreamProducer] Rejected message sending because producer is stopped. topic={}", topicName);
            throw new IllegalStateException("RedissonStreamProducer is not running (stopped or starting)");
        }
        Assert.hasText(topicName, "StreamKey must not be blank");
        Assert.notNull(data, "Message data must not be null");

        long startTime = System.currentTimeMillis();
        try {
            RStream<String, Object> stream = redissonClient.getStream(topicName);
            Map<String, Object> entryMap = Collections.singletonMap("payload", data);
            // 添加消息
            StreamMessageId id = stream.add(StreamAddArgs.entries(entryMap));
            log.info("[StreamProducer] Message sent topic={}, id={}, cost={}ms", topicName, id, (System.currentTimeMillis() - startTime));
            return id;
        } catch (Exception e) {
            log.error("[StreamProducer] Message push failed topic={}, data={}, error={}", topicName, data, e.getMessage(), e);
            throw new RuntimeException("Stream push failed", e);
        }
    }

    @Override
    public void stop() {
        log.info("[StreamProducer] Shutting down...");
        this.isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }


}
