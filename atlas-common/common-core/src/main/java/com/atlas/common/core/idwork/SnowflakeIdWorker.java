package com.atlas.common.core.idwork;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description
 * @Author ys
 * @Date 2023/4/28 16:48
 */
public class SnowflakeIdWorker {

    //开始时间截
    private final long epoch = 1714273427860L;

    //工作id所占的位数
    private final long workerIdBits = 10L;

    //序列在id中占的位数
    private final long sequenceBits = 12L;

    //支持的最大机器id，结果是1023 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
    private final long maxWorkerId = ~(-1L << workerIdBits);

    //机器ID向左移12位
    private final long workerIdShift = sequenceBits;

    //时间截向左移22位(10+12)
    private final long timestampLeftShift = sequenceBits + workerIdBits;

    private final long sequenceMask = ~(-1L << sequenceBits);

    //工作机器ID(0~1024)
    private long workerId;

    //毫秒内序列(0~4095)
    private long sequence = 0L;

    //上次生成ID的时间截
    private long lastTimestamp = -1L;

    private final long allowedOffsetMillis = 5L;

    private Lock lock = new ReentrantLock(false);

    public SnowflakeIdWorker(WorkIdService workIdService) {
        this.workerId = workIdService.getWorkId();
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("workerId can't be greater than %d or less than 0", maxWorkerId));
        }
    }

    public long nextId() {
        lock.lock();
        try {
            long currentTimestamp = currentTimestamp();
            // 处理时钟回拨
            if (currentTimestamp < lastTimestamp) {
                long offset = lastTimestamp - currentTimestamp;
                if (offset <= allowedOffsetMillis) {
                    try {
                        Thread.sleep(offset);
                        currentTimestamp = currentTimestamp();
                        // 如果醒来发现还是回退的，说明回退严重，抛出异常
                        if (currentTimestamp < lastTimestamp) {
                            throw new RuntimeException("Clock still backwards after waiting.");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new RuntimeException(
                            String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - currentTimestamp));
                }
            }
            //同一时间生成，进行序列递增,否则毫秒内序列重置
            if (currentTimestamp == lastTimestamp) {
                //如果毫秒相同，则从0递增生成序列号
                sequence = (sequence + 1) & sequenceMask;
                //如果毫秒内序列溢出，则阻塞到下一毫秒，获取新的时间戳
                if (sequence == 0) {
                    currentTimestamp = waitNextMillis(currentTimestamp);
                }
            } else {
                sequence = ThreadLocalRandom.current().nextLong(0, 5);
            }
            lastTimestamp = currentTimestamp;
            // 位运算组合 ID
            return ((currentTimestamp - this.epoch) << this.timestampLeftShift)
                    | (this.workerId << this.workerIdShift)
                    | this.sequence;
        } finally {
            lock.unlock();
        }

    }


    private long waitNextMillis(long lastTimestamp) {
        long mill = currentTimestamp();
        while (mill <= lastTimestamp) {
            mill = currentTimestamp();
        }
        return mill;
    }

    private long currentTimestamp() {
        return System.currentTimeMillis();
    }

}
