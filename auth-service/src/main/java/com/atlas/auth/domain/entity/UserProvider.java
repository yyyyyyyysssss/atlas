package com.atlas.auth.domain.entity;

import java.time.LocalDateTime;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Tolerate;
import com.atlas.common.mybatis.entity.BaseIdEntity;

/**
 * (UserProvider)实体类
 *
 * @author ys
 * @since 2026-05-08 17:33:59
 */
@Getter
@Setter
@TableName(value = "user_provider", autoResultMap = true)
@Builder
public class UserProvider extends BaseIdEntity {

    @Tolerate
    public UserProvider() {
    }

    // 用户ID 
    @TableField("user_id")
    private Long userId;

    // 身份类型 (GOOGLE, GITHUB,ATLAS) 
    @TableField("provider")
    private String provider;

    // 唯一标识 (如OpenID, UnionID, Sub, 手机号) 
    @TableField("provider_user_id")
    private String providerUserId;

    // 是否已验证 (false:未验证, true:已验证)
    @TableField("verified")
    private Boolean verified;

    // 扩展信息 
    @TableField(value = "extra_info", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraInfo;

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    protected LocalDateTime createTime;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    protected LocalDateTime updateTime;
}

