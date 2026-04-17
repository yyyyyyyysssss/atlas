package com.atlas.user.mapper;

import com.atlas.user.domain.entity.WorkSchedule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (WorkSchedule)表数据库访问层
 *
 * @author ys
 * @since 2026-04-17 15:44:54
 */
@Mapper
public interface WorkScheduleMapper extends BaseMapper<WorkSchedule> {
    
}

