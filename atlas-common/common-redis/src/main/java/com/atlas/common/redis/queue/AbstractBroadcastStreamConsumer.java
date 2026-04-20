package com.atlas.common.redis.queue;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

import java.util.UUID;

/**
 * @Description 广播模式：每个实例都会创建一个独立的消费组，确保每个实例都能收到完整消息
 * @Author ys
 * @Date 2026/4/20 13:23
 */
@Slf4j
public abstract class AbstractBroadcastStreamConsumer<T> extends AbstractStreamConsumer<T>{

    private final String dynamicGroupName;

    protected AbstractBroadcastStreamConsumer(RedissonClient redissonClient, String topicName) {
        // 使用 "broadcast" 作为固定前缀，配合 UUID 保证唯一性
        // 这样在 Redis 里看到名字就知道这是个广播组
        this(redissonClient, topicName, "broadcast");
    }

    protected AbstractBroadcastStreamConsumer(RedissonClient redissonClient, String topicName, String groupPrefix) {
        // 关键点：基础组名 + 唯一标识 = 广播组
        super(redissonClient, topicName, groupPrefix + ":" + UUID.randomUUID().toString().substring(0, 6));
        this.dynamicGroupName = this.getGroupName();
    }

    @Override
    public void stop() {
        //  先调用父类停止拉取线程和线程池
        super.stop();
        //  清理 Redis 中的临时消费组（非常重要！）
        try {
            getRedissonClient().getStream(getTopicName()).removeGroup(dynamicGroupName);
            log.info("[Broadcast] Clean up dynamic group {}", dynamicGroupName);
        } catch (Exception e) {
            log.warn("[Broadcast] Failed to remove dynamic group {}", dynamicGroupName);
        }
    }

}
