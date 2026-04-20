package com.atlas.user.service.impl;

import com.atlas.common.core.api.notification.NotificationApi;
import com.atlas.common.core.api.notification.builder.NotificationRequest;
import com.atlas.common.core.api.notification.enums.NotificationCategory;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.common.core.queue.DelayMessagePublisher;
import com.atlas.common.mybatis.utils.TransactionUtils;
import com.atlas.user.consumer.handler.WorkScheduleDelayHandler;
import com.atlas.user.domain.dto.WorkScheduleCreateDTO;
import com.atlas.user.domain.dto.WorkScheduleTaskDTO;
import com.atlas.user.domain.dto.WorkScheduleUpdateDTO;
import com.atlas.user.domain.entity.WorkSchedule;
import com.atlas.user.domain.vo.WorkScheduleVO;
import com.atlas.user.mapper.WorkScheduleMapper;
import com.atlas.user.mapping.WorkScheduleMapping;
import com.atlas.user.service.WorkScheduleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * (WorkSchedule)表服务实现类
 *
 * @author ys
 * @since 2026-04-17 15:44:54
 */
@Service("workScheduleService")
@Slf4j
public class WorkScheduleServiceImpl extends ServiceImpl<WorkScheduleMapper, WorkSchedule> implements WorkScheduleService {

    @Resource
    private WorkScheduleMapper workScheduleMapper;

    @Resource
    private DelayMessagePublisher delayMessagePublisher;

    @Resource
    private NotificationApi notificationApi;

    @Override
    public List<WorkScheduleVO> getWorkScheduleByRange(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        List<WorkSchedule> list = this.lambdaQuery()
                .eq(WorkSchedule::getUserId, userId) // 匹配用户ID
                .between(WorkSchedule::getStartTime, start, end)
                .orderByAsc(WorkSchedule::getStartTime) // 按时间先后排序，方便前端 Timeline 展示
                .list();
        return WorkScheduleMapping.INSTANCE.toWorkScheduleVO(list);
    }

    @Override
    @Transactional
    public Long createWorkSchedule(Long userId, WorkScheduleCreateDTO createDTO) {
        WorkSchedule entity = WorkScheduleMapping.INSTANCE.toWorkSchedule(createDTO);
        entity.setId(IdGen.genId());
        entity.setUserId(userId);
        int row = workScheduleMapper.insert(entity);
        if (row <= 0) {
            throw new BusinessException("创建失败");
        }
        // 处理延迟提醒逻辑
        TransactionUtils.executeAfterCommit(() -> sendDelayNotify(entity));
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateWorkSchedule(Long userId, WorkScheduleUpdateDTO updateDTO, boolean isFullUpdate) {
        WorkSchedule entity = checkAndResult(updateDTO.getId());
        LocalDateTime oldStartTime = entity.getStartTime();
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException("修改失败，非本人事项");
        }
        if (isFullUpdate) {
            WorkScheduleMapping.INSTANCE.overwriteWorkSchedule(updateDTO, entity);
        } else {
            WorkScheduleMapping.INSTANCE.updateWorkSchedule(updateDTO, entity);
        }
        int row = workScheduleMapper.updateById(entity);

        if (row <= 0) {
            throw new BusinessException("修改失败");
        }
        if (!oldStartTime.equals(entity.getStartTime())) {
            // 处理延迟提醒逻辑
            TransactionUtils.executeAfterCommit(() -> sendDelayNotify(entity));
        }
    }

    @Override
    @Transactional
    public void deleteWorkSchedule(Long userId, Long id) {
        WorkSchedule entity = checkAndResult(id);
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException("删除失败，非本人事项");
        }
        workScheduleMapper.deleteById(id);
    }


    private void sendDelayNotify(WorkSchedule entity) {
        int minutesBefore = 10;
        // 计算提醒的具体时间点：开始时间 - 10分钟
        LocalDateTime remindTime = entity.getStartTime().minusMinutes(minutesBefore);
        // 计算距离现在的延迟毫秒数
        long delay = Duration.between(LocalDateTime.now(), remindTime).toMillis();
        // 只有在提醒时间点晚于当前时间时，才投递任务
        if (delay > 0) {
            WorkScheduleTaskDTO task = new WorkScheduleTaskDTO();
            task.setId(entity.getId());
            task.setStartTime(entity.getStartTime());
            delayMessagePublisher.publish(WorkScheduleDelayHandler.TOPIC, task, delay, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void processScheduleRemind(WorkScheduleTaskDTO task) {
        log.info("开始处理日程提醒任务: scheduleId={}, planTime={}", task.getId(), task.getStartTime());
        // 获取数据库最新状态
        WorkSchedule entity = workScheduleMapper.selectById(task.getId());
        // 校验逻辑（防止过期任务、已删除任务执行）
        if (entity == null) {
            log.warn("日程不存在或已被删除，取消提醒: id={}", task.getId());
            return;
        }
        // 如果数据库里的开始时间与任务消息里的不一致，说明用户在此期间修改过时间
        if (!entity.getStartTime().equals(task.getStartTime())) {
            log.info("日程开始时间已变更，当前任务已失效: id={}, dbTime={}, taskTime={}",
                    entity.getId(), entity.getStartTime(), task.getStartTime());
            return;
        }
        try {
            notificationApi.send(
                    NotificationRequest
                            .text(entity.getTitle(), entity.getContent())
                            .inbox()
                            .category(NotificationCategory.TODO)
                            .noRecord()
                            .to()
                            .toUserIds(entity.getUserId())
                            .build()
            );
            log.info("日程提醒已投递至通知引擎: id={}", task.getId());
        } catch (Exception e) {
            log.error("日程提醒发送异常: id={}", task.getId(), e);
        }

    }

    private WorkSchedule checkAndResult(Long id) {
        WorkSchedule entity = workScheduleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("不存在");
        }
        return entity;
    }

}

