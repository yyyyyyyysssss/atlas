package com.atlas.user.domain.entity;

import com.atlas.common.mybatis.entity.BaseIdEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Tolerate;

/**
 * (RoleDataScope)实体类
 *
 * @author ys
 * @since 2026-03-17 13:31:51
 */
@Getter
@Setter
@TableName(value = "role_data_scope", autoResultMap = true)
@Builder
@AllArgsConstructor
public class RoleDataScope extends BaseIdEntity {

    @Tolerate
    public RoleDataScope() {
    }

    @TableField("role_id")
    private Long roleId;

    @TableField("org_id")
    private Long orgId;


}

