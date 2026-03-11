package com.atlas.user.domain.vo;

import com.atlas.user.enums.PositionStatus;
import com.atlas.user.enums.PositionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PositionVO {


    private Long id;

    // 组织id 
    private Long orgId;

    // 岗位名称 
    private String posName;

    // 岗位编码 
    private String posCode;

    // 岗位级别 
    private Integer level;

    // 状态 ACTIVE: 启用 INACTIVE: 停用 
    private PositionStatus status;

    private PositionType type;

    // 岗位备注 
    private String remark;

    // 创建人id 
    private Long creatorId;

    private String createTime;

    // 创建人姓名 
    private String creatorName;

    // 更新时间 
    private String updateTime;

    // 更新人id 
    private Long updaterId;

    // 更新人姓名 
    private String updaterName;

    public String statusName(){

        return status != null ? status.getDescription() : null;
    }

    public String typeName(){

        return type != null ? type.getDescription() : null;
    }

}

