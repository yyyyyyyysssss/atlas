package com.atlas.user.domain.dto;

import com.atlas.common.mybatis.dto.PageQueryDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PositionQueryDTO extends PageQueryDTO {

    private Long orgId;

    private String orgPath;

    private String keyword;

    private String status;

    private Boolean includeChildren;

}

