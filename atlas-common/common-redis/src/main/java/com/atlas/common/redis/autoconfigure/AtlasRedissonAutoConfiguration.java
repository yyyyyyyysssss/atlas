package com.atlas.common.redis.autoconfigure;

import com.atlas.common.redis.lock.DistributedLock;
import com.atlas.common.redis.lock.RedissonDistributedLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration(after = AtlasRedisAutoConfiguration.class)
public class AtlasRedissonAutoConfiguration {

    @Bean
    public DistributedLock distributedLock(RedissonClient redissonClient) {

        return new RedissonDistributedLock(redissonClient);
    }

}
