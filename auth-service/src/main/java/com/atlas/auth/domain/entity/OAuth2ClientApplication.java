package com.atlas.auth.domain.entity;

import com.atlas.common.mybatis.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName(value = "oauth2_client_application", autoResultMap = true)
@Builder
public class OAuth2ClientApplication extends BaseEntity {

    @Tolerate
    public OAuth2ClientApplication() {
    }

    @TableField("registered_client_id")
    private String registeredClientId;

    @TableField("client_id")
    private String clientId;

    @TableField("application_name")
    private String applicationName;

    @TableField("logo_url")
    private String logoUrl;

    @TableField("home_page_url")
    private String homePageUrl;

    @TableField("privacy_policy_url")
    private String privacyPolicyUrl;

    @TableField("terms_service_url")
    private String termsServiceUrl;

    @TableField("developer_name")
    private String developerName;

    @TableField("developer_email")
    private String developerEmail;

    @TableField("description")
    private String description;

    @TableField("last_used_time")
    private LocalDateTime lastUsedTime;

}
