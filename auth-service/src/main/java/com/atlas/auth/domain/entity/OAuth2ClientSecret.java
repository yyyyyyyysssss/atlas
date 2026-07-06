package com.atlas.auth.domain.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Tolerate;
import com.atlas.common.mybatis.entity.BaseIdEntity;

/**
 * (Oauth2ClientSecret)实体类
 *
 * @author ys
 * @since 2026-07-06 15:09:26
 */
@Getter
@Setter
@TableName(value = "oauth2_client_secret", autoResultMap = true)
@Builder
public class OAuth2ClientSecret extends BaseIdEntity {

    @Tolerate
    public OAuth2ClientSecret() {
    }

    // 关联 oauth2_registered_client.id 
    @TableField("registered_client_id")
    private String registeredClientId;

    @TableField("client_secret")
    private String clientSecret;

    // 密钥过期时间，NULL表示永不过期 
    @TableField("client_secret_expires_at")
    private LocalDateTime clientSecretExpiresAt;

    // 密钥后四位明文，用于前端展示 
    @TableField("client_secret_hint")
    private String clientSecretHint;

    @TableField("create_time")
    private LocalDateTime createTime;


}

