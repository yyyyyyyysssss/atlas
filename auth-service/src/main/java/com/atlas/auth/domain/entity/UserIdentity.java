package com.atlas.auth.domain.entity;

import java.util.Map;
import java.util.Map;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Tolerate;
import com.atlas.common.mybatis.entity.BaseIdEntity;

/**
 * (UserIdentity)实体类
 *
 * @author ys
 * @since 2026-05-08 17:33:59
 */
@Getter
@Setter
@TableName(value = "user_identity", autoResultMap = true)
@Builder
public class UserIdentity extends BaseIdEntity {

    @Tolerate
    public UserIdentity() {
    }

    // 用户ID 
    @TableField("user_id")
    private Long userId;

    // 身份类型 (GOOGLE, GITHUB,ATLAS) 
    @TableField("identity_type")
    private String identityType;

    // 唯一标识 (如OpenID, UnionID, Sub, 手机号) 
    @TableField("identifier")
    private String identifier;

    // 是否已验证 (false:未验证, true:已验证)
    @TableField("verified")
    private Boolean verified;

    // 扩展信息 
    @TableField(value = "extra_info", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraInfo;


}

