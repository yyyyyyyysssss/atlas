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
 * @Date 2023/7/18 13:27
 */

@Getter
@Setter
@TableName("im_user_role")
@Builder
public class UserRole extends BaseIdEntity {

    @Tolerate
    public UserRole(){
    }

    @TableField("user_id")
    private Long userId;

    @TableField("role_id")
    private Long roleId;


}
