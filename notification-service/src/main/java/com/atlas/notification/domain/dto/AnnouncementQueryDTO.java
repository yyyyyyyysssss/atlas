package com.atlas.notification.domain.dto;

import com.atlas.common.mybatis.dto.PageQueryDTO;
import com.atlas.notification.enums.AnnouncementStatus;
import com.atlas.notification.enums.AnnouncementType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnouncementQueryDTO extends PageQueryDTO {

    private AnnouncementStatus status;

    private AnnouncementType type;

    private String title;

}

