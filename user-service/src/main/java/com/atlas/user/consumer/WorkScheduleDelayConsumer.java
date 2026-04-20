package com.atlas.user.consumer;

import com.atlas.common.redis.queue.AbstractDelayQueueConsumer;
import com.atlas.user.consumer.handler.WorkScheduleDelayHandler;
import com.atlas.user.domain.dto.WorkScheduleTaskDTO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WorkScheduleDelayConsumer extends AbstractDelayQueueConsumer<WorkScheduleTaskDTO> {


    public WorkScheduleDelayConsumer(RedissonClient redissonClient, WorkScheduleDelayHandler workScheduleDelayHandler) {
        // 直接将 handler 传给父类，父类会自动获取 Topic 并开启监听
        super(redissonClient, workScheduleDelayHandler);
    }

}
