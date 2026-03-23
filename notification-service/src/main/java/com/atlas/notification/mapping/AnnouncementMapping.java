package com.atlas.notification.mapping;

import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;
import com.atlas.notification.domain.entity.Announcement;
import com.atlas.notification.domain.dto.AnnouncementCreateDTO;
import com.atlas.notification.domain.dto.AnnouncementUpdateDTO;
import com.atlas.notification.domain.vo.AnnouncementVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(builder = @Builder(disableBuilder = true), uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface AnnouncementMapping{
    
    AnnouncementMapping INSTANCE = Mappers.getMapper(AnnouncementMapping.class);
    
    AnnouncementVO toAnnouncementVO(Announcement entity);
    
    @IterableMapping(elementTargetType = AnnouncementVO.class)
    List<AnnouncementVO> toAnnouncementVO(List<Announcement> list);
    
    Announcement toAnnouncement(AnnouncementCreateDTO createDTO);
    
    Announcement toAnnouncement(AnnouncementUpdateDTO updateDTO);
    
    //部分更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateAnnouncement(AnnouncementUpdateDTO updateDTO, @MappingTarget Announcement entity);

    //全量更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void overwriteAnnouncement(AnnouncementUpdateDTO updateDTO, @MappingTarget Announcement entity);
}

