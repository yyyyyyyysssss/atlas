package com.atlas.common.redis.queue;

import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public abstract class AbstractDelayQueueConsumer<T> implements CommandLineRunner {

    private final RedissonClient redissonClient;

    private final String topicName;

    // 使用虚拟线程执行器：每个任务都会创建一个新的虚拟线程
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    protected AbstractDelayQueueConsumer(RedissonClient redissonClient, String topicName) {
        this.redissonClient = redissonClient;
        this.topicName = topicName;
    }

    /**
     * 业务执行逻辑
     */
    protected abstract void execute(T data);

    @Override
    public void run(String... args) {
        // 启动监听线程
        Thread.ofVirtual().name("v-consumer-monitor-" + topicName).start(() -> {
            log.info("启动虚拟线程延迟队列监听: {}", topicName);
            RBlockingDeque<T> mainQueue = redissonClient.getBlockingDeque(topicName);
            // 激活延迟队列搬运机制
            redissonClient.getDelayedQueue(mainQueue);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 阻塞获取任务（此时虚拟线程会挂起，不占用系统内核线程）
                    T data = mainQueue.take();
                    // 异步提交给虚拟线程池处理业务逻辑
                    executor.submit(() -> {
                        try {
                            execute(data);
                        } catch (Exception e) {
                            log.error("虚拟线程执行业务异常, topic={}, data={}", topicName, data, e);
                        }
                    });
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    break;
                } catch (RedissonShutdownException redissonShutdownException){
                    log.info("Redisson 已关闭，停止队列监听: {}", topicName);
                    break;
                } catch (Exception e) {
                    log.error("监听器循环异常, topic={}", topicName, e);
                }
            }
        });
    }

}
