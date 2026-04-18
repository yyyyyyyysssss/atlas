package com.atlas.common.redis.autoconfigure;

import com.atlas.common.redis.lock.DistributedLock;
import com.atlas.common.redis.lock.RedissonDistributedLock;
import com.atlas.common.redis.queue.DistributedDelayQueue;
import com.atlas.common.redis.queue.RedissonDistributedDelayQueue;
import org.redisson.api.RedissonClient;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {
        AtlasRedisAutoConfiguration.class,
        RedissonAutoConfiguration.class // 显式等待官方 Starter 完成初始化
})
public class AtlasRedissonAutoConfiguration {


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

}
