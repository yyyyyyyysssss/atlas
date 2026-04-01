package com.atlas.notification.mapping;

import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;
import com.atlas.notification.domain.entity.Notification;
import com.atlas.notification.domain.vo.NotificationVO;
import org.mapstruct.Builder;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(builder = @Builder(disableBuilder = true), uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface NotificationMapping {
    
    NotificationMapping INSTANCE = Mappers.getMapper(NotificationMapping.class);
    
    NotificationVO toNtfNotificationVO(Notification entity);
    
    @IterableMapping(elementTargetType = NotificationVO.class)
    List<NotificationVO> toNtfNotificationVO(List<Notification> list);
}

