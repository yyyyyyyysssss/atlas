package com.atlas.common.core.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/20 16:46
 */
@Slf4j
public class DelayMessagePublisher {

    private final DistributedDelayQueue delayQueue;

    public DelayMessagePublisher(DistributedDelayQueue delayQueue){
        this.delayQueue = delayQueue;
    }

    public <T> void publish(String topic, T payload, long delay, TimeUnit unit) {
        Assert.hasText(topic, "Topic (queue name) must not be empty");
        Assert.notNull(payload, "Payload must not be null");
        Assert.notNull(unit, "TimeUnit must not be null");
        try {
            log.info("[DelayPublisher] 投递延迟任务 Topic: {}, 延迟: {} {}",
                    topic, delay, unit);
            // 调用底层具体实现
            delayQueue.sendMessage(topic, payload, delay, unit);
        } catch (Exception e) {
            // 4. 异常统一处理：避免底层中间件异常直接导致业务代码崩溃，记录关键信息
            log.error("[DelayPublisher] 延迟任务投递失败! Topic: {}, 原因: {}",
                    topic, e.getMessage(), e);
            throw new RuntimeException("Failed to push delayed message to queue: " + topic, e);
        }
    }

    /**
     * 发送延迟消息（便捷重载方法，默认单位为秒）
     */
    public <T> void publish(String topic, T payload, long delayInSeconds) {
        this.publish(topic, payload, delayInSeconds, TimeUnit.SECONDS);
    }

}
