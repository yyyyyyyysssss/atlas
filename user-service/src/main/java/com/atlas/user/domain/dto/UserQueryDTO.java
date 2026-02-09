package com.atlas.user.domain.dto;

import com.atlas.common.mybatis.dto.PageQueryDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/3 15:42
 */
@Getter
@Setter
public class UserQueryDTO extends PageQueryDTO {

    private String keyword;

    private List<Long> ids;

    private Boolean enabled;

}
