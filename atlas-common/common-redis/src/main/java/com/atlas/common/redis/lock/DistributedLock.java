package com.atlas.common.redis.lock;

import java.util.concurrent.TimeUnit;

public interface DistributedLock {

    /**
     * 阻塞获取锁
     */
    LockHandle lock(String key);

    /**
     * 尝试获取锁
     * 不等待，使用 watchdog
     */
    default LockHandle tryLock(String key) {
        return tryLock(
                key,
                0,
                -1,
                TimeUnit.SECONDS
        );
    }

    /**
     * 尝试获取锁
     */
    LockHandle tryLock(
            String key,
            long waitTime,
            long leaseTime,
            TimeUnit unit
    );

}
