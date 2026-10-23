package com.atlas.auth.domain.dto;

import com.atlas.common.mybatis.dto.PageQueryDTO;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/23 9:37
 */
@Getter
@Setter
public class ApplicationQueryDTO extends PageQueryDTO {

    private String applicationName;

}
