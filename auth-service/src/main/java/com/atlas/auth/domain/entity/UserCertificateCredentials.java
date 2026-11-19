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
 *@Description
 *@Author ys
 *@Date 2026/11/19 13:52
 */
@Getter
@Setter
@TableName(value = "user_certificate_credentials", autoResultMap = true)
@Builder
public class UserCertificateCredentials extends BaseIdEntity {

    @Tolerate
    public UserCertificateCredentials() {
    }

    // 用户id
    @TableField("user_id")
    private Long userId;

    @TableField("fingerprint")
    private String fingerprint;

    @TableField("serial_number")
    private String serialNumber;

    @TableField("subject")
    private String subject;

    @TableField("issuer")
    private String issuer;

    @TableField("not_before")
    private LocalDateTime notBefore;

    @TableField("not_after")
    private LocalDateTime notAfter;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("revoked_time")
    private LocalDateTime revokedTime;

    @TableField("revoke_reason")
    private String revokeReason;

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
