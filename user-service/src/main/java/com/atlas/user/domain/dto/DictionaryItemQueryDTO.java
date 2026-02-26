package com.atlas.user.domain.dto;


import com.atlas.common.mybatis.dto.PageQueryDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DictionaryItemQueryDTO extends PageQueryDTO {

    private Long dictId;

    private String keyword;

    private Boolean enabled;

}
