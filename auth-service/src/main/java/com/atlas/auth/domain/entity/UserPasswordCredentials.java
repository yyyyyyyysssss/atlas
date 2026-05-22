package com.atlas.auth.domain.entity;

import com.atlas.common.mybatis.entity.BaseIdEntity;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;

/**
 * (UserPasswordCredentials)实体类
 *
 * @author ys
 * @since 2026-05-22 09:09:13
 */
@Getter
@Setter
@TableName(value = "user_password_credentials", autoResultMap = true)
@Builder
public class UserPasswordCredentials extends BaseIdEntity {

    @Tolerate
    public UserPasswordCredentials() {
    }

    @TableField("user_id")
    private Long userId;

    // 密码 
    @TableField("password")
    private String password;

    // 最后修改时间 
    @TableField("last_changed_time")
    private LocalDateTime lastChangedTime;

    // 强制过期时间 
    @TableField("expired_time")
    private LocalDateTime expiredTime;

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    protected LocalDateTime createTime;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    protected LocalDateTime updateTime;


}

