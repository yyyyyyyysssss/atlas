package com.atlas.auth.domain.entity;

import java.time.LocalDateTime;
import java.time.LocalDateTime;

import com.atlas.auth.enums.IdentifierStatus;
import com.atlas.auth.enums.IdentifierType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Tolerate;
import com.atlas.common.mybatis.entity.BaseIdEntity;

/**
 * (UserIdentifier)实体类
 *
 * @author ys
 * @since 2026-05-18 09:56:57
 */
@Getter
@Setter
@TableName(value = "user_identifier", autoResultMap = true)
@Builder
public class UserIdentifier extends BaseIdEntity {

    @Tolerate
    public UserIdentifier() {
    }

    // 用户id 
    @TableField("user_id")
    private Long userId;

    // 标识类型：USERNAME / EMAIL / PHONE 
    @TableField("identifier_type")
    private IdentifierType identifierType;

    // 标识值 
    @TableField("identifier_value")
    private String identifierValue;

    // 标准化值（用于唯一查找） 
    @TableField("normalized_value")
    private String normalizedValue;

    // ACTIVE / DISABLED / DELETED 
    @TableField("status")
    private IdentifierStatus status;

    // 是否已验证 
    @TableField("verified")
    private Boolean verified;

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    protected LocalDateTime createTime;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    protected LocalDateTime updateTime;


}

