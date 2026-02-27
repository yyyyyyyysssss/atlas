package com.atlas.user.domain.entity;

import com.atlas.common.mybatis.entity.BaseIdEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

/**
 * @Description
 * @Author ys
 * @Date 2023/7/18 13:29
 */
@Getter
@Setter
@TableName("role_authority")
@Builder
public class RoleAuthority extends BaseIdEntity {

    @Tolerate
    public RoleAuthority(){
    }

    @TableField("role_id")
    private Long roleId;

    @TableField("authority_id")
    private Long authorityId;


}
