package com.atlas.user.domain.dto;

import com.atlas.user.enums.PositionStatus;
import com.atlas.user.enums.PositionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PositionCreateDTO {

    // 组织id
    @NotNull(message = "组织id不能为空")
    private Long orgId;

    // 岗位名称
    @NotBlank(message = "岗位名称不能为空")
    private String posName;

    // 岗位编码 
    private String posCode;

    @NotNull(message = "岗位类型不能为空")
    private PositionType type;

    // 岗位级别 
    private Integer level;

    // 状态 ACTIVE: 启用 INACTIVE: 停用 
    private PositionStatus status = PositionStatus.ACTIVE;

    // 岗位备注 
    private String remark;


}

