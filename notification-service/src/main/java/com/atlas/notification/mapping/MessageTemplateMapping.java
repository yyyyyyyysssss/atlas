package com.atlas.notification.mapping;

import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;
import com.atlas.notification.domain.dto.MessageTemplateCreateDTO;
import com.atlas.notification.domain.dto.MessageTemplateUpdateDTO;
import com.atlas.notification.domain.entity.MessageTemplate;
import com.atlas.notification.domain.vo.MessageTemplateVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(builder = @Builder(disableBuilder = true), uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface MessageTemplateMapping {
    
    MessageTemplateMapping INSTANCE = Mappers.getMapper(MessageTemplateMapping.class);
    
    MessageTemplateVO toMessageTemplateVO(MessageTemplate entity);
    
    @IterableMapping(elementTargetType = MessageTemplateVO.class)
    List<MessageTemplateVO> toMessageTemplateVO(List<MessageTemplate> list);
    
    MessageTemplate toMessageTemplate(MessageTemplateCreateDTO createDTO);
    
    MessageTemplate toMessageTemplate(MessageTemplateUpdateDTO updateDTO);
    
    //部分更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateMessageTemplate(MessageTemplateUpdateDTO updateDTO, @MappingTarget MessageTemplate entity);

    //全量更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void overwriteMessageTemplate(MessageTemplateUpdateDTO updateDTO, @MappingTarget MessageTemplate entity);
}

