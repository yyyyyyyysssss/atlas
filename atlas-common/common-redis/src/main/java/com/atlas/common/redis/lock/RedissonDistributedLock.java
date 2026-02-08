package com.atlas.common.redis.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

@Slf4j
public class RedissonDistributedLock implements DistributedLock{

    private RedissonClient redissonClient;

    public RedissonDistributedLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public RLock lock(String key) {
        RLock lock = getLock(key);
        lock.lock(); // 默认开启看门狗，永不超时直到手动解锁
        return lock;
    }

    @Override
    public RLock lock(String key, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(key);
        lock.lock(leaseTime, unit); // 不使用看门狗
        return lock;
    }

    @Override
    public RLock tryLock(String key) {
        try {
            RLock lock = getLock(key);
            return lock.tryLock() ? lock : null;
        } catch (Exception e) {
            log.error("tryLock error key={}", key, e);
            return null;
        }
    }

    @Override
    public RLock tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        try {
            RLock lock = redissonClient.getLock(key);
            // leaseTime 为 -1 时启用看门狗续期机制
            boolean success = lock.tryLock(waitTime, leaseTime, unit);
            return success ? lock : null;
        } catch (InterruptedException e) {
            // 恢复中断状态，让上层调用者知道线程被中断了
            Thread.currentThread().interrupt();
            // 记录警告
            log.warn("分布式锁获取因线程中断而终止, key={}", key);
            return null;
        } catch (Exception e) {
            log.error("tryLock error key={}", key, e);
            return null;
        }
    }

    @Override
    public LockHandle lockAuto(String key) {
        RLock lock = lock(key);
        return new RedissonLockHandle(lock);
    }

    @Override
    public LockHandle lockAuto(String key, long leaseTime, TimeUnit unit) {
        RLock lock = lock(key, leaseTime, unit);
        return new RedissonLockHandle(lock);
    }

    @Override
    public LockHandle tryLockAuto(String key, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = tryLock(key, waitTime, leaseTime, unit);
        if (lock == null) return null;
        return new RedissonLockHandle(lock);
    }

    private RLock getLock(String key) {
        return redissonClient.getLock(key);
    }

    /**
     * 内部包装类，增强 unlock 的安全性
     */
    private record RedissonLockHandle(RLock lock) implements LockHandle {
        @Override
        public void close() {
            // 只有当锁还在当前线程手中时才解锁，防止抛出 IllegalMonitorStateException
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
