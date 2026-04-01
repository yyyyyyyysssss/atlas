package com.atlas.notification.mapping;

import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;
import com.atlas.notification.domain.dto.NotificationTemplateCreateDTO;
import com.atlas.notification.domain.dto.NotificationTemplateUpdateDTO;
import com.atlas.notification.domain.entity.NotificationTemplate;
import com.atlas.notification.domain.vo.NotificationTemplateVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(builder = @Builder(disableBuilder = true), uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface NotificationTemplateMapping {
    
    NotificationTemplateMapping INSTANCE = Mappers.getMapper(NotificationTemplateMapping.class);
    
    NotificationTemplateVO toMessageTemplateVO(NotificationTemplate entity);
    
    @IterableMapping(elementTargetType = NotificationTemplateVO.class)
    List<NotificationTemplateVO> toMessageTemplateVO(List<NotificationTemplate> list);
    
    NotificationTemplate toMessageTemplate(NotificationTemplateCreateDTO createDTO);
    
    NotificationTemplate toMessageTemplate(NotificationTemplateUpdateDTO updateDTO);
    
    //部分更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateMessageTemplate(NotificationTemplateUpdateDTO updateDTO, @MappingTarget NotificationTemplate entity);

    //全量更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void overwriteMessageTemplate(NotificationTemplateUpdateDTO updateDTO, @MappingTarget NotificationTemplate entity);
}

