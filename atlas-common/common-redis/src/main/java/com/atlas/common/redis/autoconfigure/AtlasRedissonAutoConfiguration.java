package com.atlas.common.redis.autoconfigure;

import com.atlas.common.core.queue.DelayMessagePublisher;
import com.atlas.common.redis.lock.DistributedLock;
import com.atlas.common.redis.lock.RedissonDistributedLock;
import com.atlas.common.core.queue.DistributedDelayQueue;
import com.atlas.common.redis.queue.RedissonDistributedDelayQueue;
import com.atlas.common.redis.queue.RedissonStreamProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {
        AtlasRedisAutoConfiguration.class,
        RedissonAutoConfiguration.class // 显式等待官方 Starter 完成初始化
})
public class AtlasRedissonAutoConfiguration {


    @Bean
    public RedissonAutoConfigurationCustomizer codecCustomizer(@Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {
        return config -> {
            // 仅仅修改序列化器，保留其他所有配置文件里的 Redis 连接设置
            config.setCodec(new JsonJacksonCodec(redisObjectMapper));
        };
    }

    @Bean
    @ConditionalOnMissingBean(DistributedLock.class)
    public DistributedLock distributedLock(RedissonClient redissonClient) {

        return new RedissonDistributedLock(redissonClient);
    }


    /**
     * 配置分布式延迟队列基座
     * 只有在 RedissonClient 存在时才加载
     */
    @Bean
    @ConditionalOnMissingBean(DistributedDelayQueue.class)
    public DistributedDelayQueue distributedDelayQueue(RedissonClient redissonClient) {

        return new RedissonDistributedDelayQueue(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean(DelayMessagePublisher.class)
    public DelayMessagePublisher delayMessagePublisher(DistributedDelayQueue distributedDelayQueue) {
        return new DelayMessagePublisher(distributedDelayQueue);
    }


    // 轻量级消息队列
    @Bean
    @ConditionalOnMissingBean(RedissonStreamProducer.class)
    public RedissonStreamProducer redissonStreamProducer(RedissonClient redissonClient) {

        return new RedissonStreamProducer(redissonClient);
    }

}
