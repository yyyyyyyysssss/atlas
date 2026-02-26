package com.atlas.user.domain.vo;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DictionaryItemVO {

    private Long id;

    private Long dictId;

    private Long parentId;

    private String label;

    private String value;

    private String alias;

    private Boolean enabled;

    private Integer level;

    private String category;

    private String imgUrl;

    private Integer sort;

    private String extJson;

    private String createTime;

    private String creatorName;

    private String updateTime;

    private String updaterName;

    private List<DictionaryItemVO> children;

}
