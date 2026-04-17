package com.atlas.user.service.impl;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.user.domain.dto.WorkScheduleCreateDTO;
import com.atlas.user.domain.dto.WorkScheduleUpdateDTO;
import com.atlas.user.domain.entity.WorkSchedule;
import com.atlas.user.domain.vo.WorkScheduleVO;
import com.atlas.user.mapper.WorkScheduleMapper;
import com.atlas.user.mapping.WorkScheduleMapping;
import com.atlas.user.service.WorkScheduleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * (WorkSchedule)表服务实现类
 *
 * @author ys
 * @since 2026-04-17 15:44:54
 */
@Service("workScheduleService")
@AllArgsConstructor
@Slf4j
public class WorkScheduleServiceImpl extends ServiceImpl<WorkScheduleMapper, WorkSchedule> implements WorkScheduleService {

    private WorkScheduleMapper workScheduleMapper;


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
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateWorkSchedule(Long userId, WorkScheduleUpdateDTO updateDTO, boolean isFullUpdate) {
        WorkSchedule entity = checkAndResult(updateDTO.getId());
        if(!entity.getUserId().equals(userId)){
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
    }

    @Override
    @Transactional
    public void deleteWorkSchedule(Long userId, Long id) {
        WorkSchedule entity = checkAndResult(id);
        if(!entity.getUserId().equals(userId)){
            throw new BusinessException("删除失败，非本人事项");
        }
        workScheduleMapper.deleteById(id);
    }

    private WorkSchedule checkAndResult(Long id) {
        WorkSchedule entity = workScheduleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("不存在");
        }
        return entity;
    }

}

