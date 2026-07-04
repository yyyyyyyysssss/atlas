package com.atlas.auth.domain.entity;

import com.atlas.common.mybatis.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

@Getter
@Setter
@TableName(value = "oauth2_application", autoResultMap = true)
@Builder
public class OAuth2Application extends BaseEntity {

    @Tolerate
    public OAuth2Application() {
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

    @TableField("description")
    private String description;

}
