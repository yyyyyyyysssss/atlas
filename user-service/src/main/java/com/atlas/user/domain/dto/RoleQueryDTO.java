package com.atlas.user.domain.dto;

import com.atlas.common.mybatis.dto.PageQueryDTO;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/3 15:42
 */
@Getter
@Setter
public class RoleQueryDTO extends PageQueryDTO {

    private String keyword;

    private Boolean enabled;

}
