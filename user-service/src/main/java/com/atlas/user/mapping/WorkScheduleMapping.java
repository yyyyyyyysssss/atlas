package com.atlas.user.mapping;

import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;
import com.atlas.user.domain.entity.WorkSchedule;
import com.atlas.user.domain.dto.WorkScheduleCreateDTO;
import com.atlas.user.domain.dto.WorkScheduleUpdateDTO;
import com.atlas.user.domain.vo.WorkScheduleVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(builder = @Builder(disableBuilder = true), uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface WorkScheduleMapping{
    
    WorkScheduleMapping INSTANCE = Mappers.getMapper(WorkScheduleMapping.class);
    
    WorkScheduleVO toWorkScheduleVO(WorkSchedule entity);
    
    @IterableMapping(elementTargetType = WorkScheduleVO.class)
    List<WorkScheduleVO> toWorkScheduleVO(List<WorkSchedule> list);
    
    WorkSchedule toWorkSchedule(WorkScheduleCreateDTO createDTO);
    
    WorkSchedule toWorkSchedule(WorkScheduleUpdateDTO updateDTO);
    
    //部分更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateWorkSchedule(WorkScheduleUpdateDTO updateDTO, @MappingTarget WorkSchedule entity);

    //全量更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void overwriteWorkSchedule(WorkScheduleUpdateDTO updateDTO, @MappingTarget WorkSchedule entity);
}

