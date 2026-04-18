package com.atlas.user.consumer;

import com.atlas.common.redis.queue.AbstractDelayQueueConsumer;
import com.atlas.user.domain.dto.WorkScheduleTaskDTO;
import com.atlas.user.service.WorkScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WorkScheduleDelayConsumer extends AbstractDelayQueueConsumer<WorkScheduleTaskDTO> {

    private final WorkScheduleService workScheduleService;

    public static final String SCHEDULE_NOTIFY_TOPIC = "WORK_SCHEDULE_NOTIFY";

    protected WorkScheduleDelayConsumer(RedissonClient redissonClient, WorkScheduleService workScheduleService) {
        super(redissonClient, SCHEDULE_NOTIFY_TOPIC);
        this.workScheduleService = workScheduleService;
    }

    @Override
    protected void execute(WorkScheduleTaskDTO task) {
        log.info("收到日程提醒任务, id: {}", task.getId());
        try {
            workScheduleService.processScheduleRemind(task);
        }catch (Exception e){
            log.error("执行日程提醒失败, id: {}", task.getId(), e);
        }
    }
}
