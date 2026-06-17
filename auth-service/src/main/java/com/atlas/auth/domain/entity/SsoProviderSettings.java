package com.atlas.auth.domain.entity;

import java.time.LocalDateTime;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.Map;

import com.atlas.auth.enums.SsoProviderProtocol;
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
 * (SsoProviderSettings)实体类
 *
 * @author ys
 * @since 2026-06-17 09:08:23
 */
@Getter
@Setter
@TableName(value = "sso_provider_settings", autoResultMap = true)
@Builder
public class SsoProviderSettings extends BaseIdEntity {

    @Tolerate
    public SsoProviderSettings() {
    }

    // 关联 sso_provider.provider 
    @TableField("provider")
    private String provider;

    // 协议: SAML2, OAUTH2
    @TableField("protocol")
    private SsoProviderProtocol protocol;

    // 协议相关的核心配置 
    @TableField(value = "settings", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> settings;

    // 是否为该提供商的首选协议: 1-是, 0-否 
    @TableField("is_primary")
    private Boolean isPrimary;

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    protected LocalDateTime createTime;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    protected LocalDateTime updateTime;


}

