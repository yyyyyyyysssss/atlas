package com.atlas.common.core.api.user.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户资料 DTO
 */
@Getter
@Setter
public class UserProfileDTO {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 用户姓名
     */
    private String fullName;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 所属组织 ID
     */
    private Long orgId;

    /**
     * 个人座右铭
     */
    private String motto;

    /**
     * 用户是否启用
     */
    private Boolean enabled;

}