package com.atlas.user.domain.entity;

import com.atlas.common.mybatis.entity.BaseEntity;
import com.atlas.user.enums.RoleType;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

/**
 * @Description
 * @Author ys
 * @Date 2023/7/18 11:25
 */

@Getter
@Setter
@TableName(value = "role",autoResultMap = true)
@Builder
public class Role extends BaseEntity {

    @Tolerate
    public Role(){
    }

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("type")
    @EnumValue
    private RoleType type;

    /**
     * 是否为超级管理员角色
     * @return true 如果是超级管理员角色
     */
    public boolean isSuperAdmin() {
        return this.type.equals(RoleType.SUPER_ADMIN);
    }

}
