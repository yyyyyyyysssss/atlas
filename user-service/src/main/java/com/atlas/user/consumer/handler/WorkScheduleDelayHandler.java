package com.atlas.user.consumer.handler;

import com.atlas.common.core.queue.DelayMessageHandler;
import com.atlas.user.domain.dto.WorkScheduleTaskDTO;
import com.atlas.user.service.WorkScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/20 17:07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkScheduleDelayHandler implements DelayMessageHandler<WorkScheduleTaskDTO> {

    private final WorkScheduleService workScheduleService;

    public static final String TOPIC = "WORK_SCHEDULE_NOTIFY";

    @Override
    public void handle(WorkScheduleTaskDTO payload) {
        log.info("收到日程提醒任务, id: {}", payload.getId());
        workScheduleService.processScheduleRemind(payload);
    }

    @Override
    public String getTopic() {
        return TOPIC;
    }
}
