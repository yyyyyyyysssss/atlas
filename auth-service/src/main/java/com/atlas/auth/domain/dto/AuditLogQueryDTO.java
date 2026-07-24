package com.atlas.auth.domain.dto;

import com.atlas.common.mybatis.dto.PageQueryDTO;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2026/7/24 15:22
 */
@Getter
@Setter
public class AuditLogQueryDTO extends PageQueryDTO {

    private Long userId;

}
