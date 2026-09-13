package com.atlas.common.redis.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

@Slf4j
public class RedissonDistributedLock implements DistributedLock{

    private final RedissonClient redissonClient;

    public RedissonDistributedLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public LockHandle lock(String key) {
        RLock lock = getLock(key);
        // 默认开启看门狗，在持锁线程存活期间自动续期，直到主动 unlock
        lock.lock();
        return new RedissonLockHandle(lock);
    }

    @Override
    public LockHandle tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        try {
            RLock lock = getLock(key);
            // leaseTime 为 -1 时启用看门狗续期机制
            boolean success = lock.tryLock(waitTime, leaseTime, unit);
            return new RedissonLockHandle(success ? lock : null);
        } catch (InterruptedException e) {
            // 恢复中断状态，让上层调用者知道线程被中断了
            Thread.currentThread().interrupt();
            // 记录警告
            log.warn("分布式锁获取因线程中断而终止, key={}", key);
            return new RedissonLockHandle(null);
        } catch (Exception e) {
            log.error("tryLock error key={}", key, e);
            return new RedissonLockHandle(null);
        }
    }

    private RLock getLock(String key) {
        return redissonClient.getLock(key);
    }
}
