package com.atlas.user.service;


import com.atlas.user.domain.dto.WorkScheduleCreateDTO;
import com.atlas.user.domain.dto.WorkScheduleUpdateDTO;
import com.atlas.user.domain.entity.WorkSchedule;
import com.atlas.user.domain.vo.WorkScheduleVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;


/**
 * (WorkSchedule)表服务接口
 *
 * @author ys
 * @since 2026-04-17 15:44:53
 */
public interface WorkScheduleService extends IService<WorkSchedule> {

    List<WorkScheduleVO> getWorkScheduleByRange(Long userId, LocalDate startDate, LocalDate endDate);

    Long createWorkSchedule(Long userId,WorkScheduleCreateDTO createDTO);

    void updateWorkSchedule(Long userId,WorkScheduleUpdateDTO updateDTO, boolean isFullUpdate);

    void deleteWorkSchedule(Long userId,Long id);
}

