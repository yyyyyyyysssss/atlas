package com.atlas.auth.domain.entity;

import com.atlas.auth.enums.ClientProtocol;
import com.atlas.common.mybatis.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/23 8:55
 */
@Getter
@Setter
@TableName(value = "application_client", autoResultMap = true)
@Builder
public class ApplicationClient extends BaseEntity {

    @Tolerate
    public ApplicationClient() {
    }

    @TableField("application_id")
    private Long applicationId;

    @TableField("registered_client_id")
    private String registeredClientId;

    @TableField("client_id")
    private String clientId;

    @TableField("client_name")
    private String clientName;

    @TableField("protocol")
    private ClientProtocol protocol;

    @TableField("description")
    private String description;

    @TableField("last_used_time")
    private LocalDateTime lastUsedTime;

}
