package com.atlas.user.domain.entity;

import com.atlas.common.mybatis.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

/**
 * @Description
 * @Author ys
 * @Date 2024/4/25 12:33
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "user", autoResultMap = true)
public class User extends BaseEntity {

    @TableField("full_name")
    private String fullName;

    @TableField("motto")
    private String motto;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("avatar")
    private String avatar;

    @TableField(value = "settings", typeHandler = JacksonTypeHandler.class)
    private UserSetting settings;
}
