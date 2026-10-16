package com.atlas.auth.domain.entity;

import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.common.mybatis.entity.BaseIdEntity;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;
import java.util.Map;

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

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    protected LocalDateTime createTime;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    protected LocalDateTime updateTime;


}

