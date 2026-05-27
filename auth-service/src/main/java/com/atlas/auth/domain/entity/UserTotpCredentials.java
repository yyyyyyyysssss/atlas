package com.atlas.auth.domain.entity;

import com.atlas.auth.enums.UserTotpStatus;
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
 * (UserTotpCredentials)实体类
 *
 * @author ys
 * @since 2026-05-27 14:56:07
 */
@Getter
@Setter
@TableName(value = "user_totp_credentials", autoResultMap = true)
@Builder
public class UserTotpCredentials extends BaseIdEntity {

    @Tolerate
    public UserTotpCredentials() {
    }

    // 用户id 
    @TableField("user_id")
    private Long userId;

    // TOTP 密钥 (AES加密存储) 
    @TableField("secret_key")
    private String secretKey;

    // UNACTIVATED：未激活 ACTIVATED：已激活 
    @TableField("status")
    private UserTotpStatus status;

    // 发行方名称 
    @TableField("issuer")
    private String issuer;

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;



}

