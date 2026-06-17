package com.atlas.auth.domain.entity;

import java.time.LocalDateTime;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.experimental.Tolerate;
import com.atlas.common.mybatis.entity.BaseIdEntity;

/**
 * (SsoProvider)实体类
 *
 * @author ys
 * @since 2026-06-17 09:06:51
 */
@Getter
@Setter
@TableName(value = "sso_provider", autoResultMap = true)
@Builder
public class SsoProvider{

    @Tolerate
    public SsoProvider() {
    }

    // 身份提供商唯一标识 
    @TableId(value = "provider", type = IdType.INPUT)
    private String provider;

    // 展示名称 (如: Auth0, Google) 
    @TableField("name")
    private String name;

    // 是否启用 
    @TableField("enabled")
    private Boolean enabled;

    // 为该提供商开启网络代理
    @TableField("use_proxy")
    private Boolean useProxy;

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    protected LocalDateTime createTime;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    protected LocalDateTime updateTime;


}

