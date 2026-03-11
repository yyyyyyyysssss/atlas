package com.atlas.user.domain.entity;

import com.atlas.common.mybatis.entity.BaseEntity;
import com.atlas.user.enums.PositionStatus;
import com.atlas.user.enums.PositionType;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Tolerate;

/**
 * (Position)实体类
 *
 * @author ys
 * @since 2026-03-10 14:29:22
 */
@Getter
@Setter
@TableName(value = "position", autoResultMap = true)
@Builder
public class Position extends BaseEntity {

    @Tolerate
    public Position() {
    }

    // 组织id 
    @TableField("org_id")
    private Long orgId;

    // 岗位名称 
    @TableField("pos_name")
    private String posName;

    // 岗位编码 
    @TableField("pos_code")
    private String posCode;

    @TableField("type")
    private PositionType type;

    // 岗位级别 
    @TableField("level")
    private Integer level;

    // 状态 ACTIVE: 启用 INACTIVE: 停用 
    @TableField("status")
    private PositionStatus status;

    // 岗位备注 
    @TableField("remark")
    private String remark;


}

