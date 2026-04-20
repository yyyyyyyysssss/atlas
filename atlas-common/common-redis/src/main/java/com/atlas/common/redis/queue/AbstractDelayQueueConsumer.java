package com.atlas.common.redis.queue;

import com.atlas.common.core.queue.DelayMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RedissonClient;
import org.springframework.context.SmartLifecycle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractDelayQueueConsumer<T> implements SmartLifecycle {

    private final RedissonClient redissonClient;

    private final DelayMessageHandler<T> handler;

    private final String topicName;

    private volatile boolean isRunning = false;

    // 使用虚拟线程执行器：每个任务都会创建一个新的虚拟线程
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    protected AbstractDelayQueueConsumer(RedissonClient redissonClient, DelayMessageHandler<T> handler) {
        this.redissonClient = redissonClient;
        this.topicName = handler.getTopic();
        this.handler = handler;
    }

    @Override
    public void start() {
        log.info("[RedissonDelayConsumer] Starting. topic={}", topicName);
        this.isRunning = true;
        // 启动监听线程
        Thread.ofVirtual().name("v-delay-monitor-" + topicName).start(this::consumeLoop);
    }

    private void consumeLoop() {
        RBlockingDeque<T> mainQueue = redissonClient.getBlockingDeque(topicName);
        // 激活延迟队列搬运机制
        redissonClient.getDelayedQueue(mainQueue);
        while (isRunning){
            try {
                // 阻塞获取任务，增加超时时间以便能响应 isRunning 状态的变化
                T data = mainQueue.poll(5, TimeUnit.SECONDS);
                if (data == null) {
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug("[RedissonDelayConsumer] Received job. topic={}, data={}", topicName, data);
                }
                // 异步提交给虚拟线程池处理业务逻辑
                executor.submit(() -> {
                    long begin = System.currentTimeMillis();
                    try {
                        handler.handle(data);
                        log.info("[RedissonDelayConsumer] Execution success. topic={}, cost={}ms",
                                topicName, System.currentTimeMillis() - begin);
                    } catch (Exception e) {
                        log.error("[RedissonDelayConsumer] Business execution failed. topic={}, data={}",
                                topicName, data, e);
                    }
                });
            } catch (InterruptedException e) {
                log.warn("[RedissonDelayConsumer] Monitor interrupted. exiting. topic={}", topicName);
                Thread.currentThread().interrupt();
                break;
            } catch (RedissonShutdownException redissonShutdownException) {
                log.info("[RedissonDelayConsumer] Redisson shutdown. topic={}", topicName);
                break;
            } catch (Exception e) {
                if (!isRunning) break;
                // 循环中的未知异常，避免高频刷屏建议增加休眠防止死循环撑爆日志
                log.error("[RedissonDelayConsumer] Unexpected loop error. topic={}", topicName, e);
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException ignore) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public void stop() {
        log.info("[RedissonDelayConsumer] Stopping. topic={}", topicName);
        this.isRunning = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("[RedissonDelayConsumer] Executor forced shutdown. topic={}", topicName);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("[RedissonDelayConsumer] stopped. topic={}", topicName);
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

}
