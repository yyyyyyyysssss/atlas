package com.atlas.common.redis.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;

@Slf4j
public class RedissonLockHandle implements LockHandle {

    private final RLock lock;

    public RedissonLockHandle(RLock lock) {
        this.lock = lock;
    }

    @Override
    public boolean acquired() {
        return lock != null;
    }

    @Override
    public void close() {
        // 只有当锁还在当前线程手中时才解锁，防止抛出 IllegalMonitorStateException
        if (lock != null && lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
            } catch (Exception e) {
                log.warn("释放分布式锁异常", e);
            }
        }
    }


}
