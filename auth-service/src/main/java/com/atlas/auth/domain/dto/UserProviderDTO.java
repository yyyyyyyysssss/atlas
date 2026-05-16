package com.atlas.auth.domain.dto;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProviderDTO {

    private Long id;

    // 用户ID 
    private Long userId;

    // 身份类型 (GOOGLE, GITHUB,ATLAS) 
    private String provider;

    // 唯一标识 (如OpenID, UnionID, Sub, 手机号) 
    private String providerUserId;

    // 是否已验证 (false:未验证, true:已验证)
    private Boolean verified;

    // 扩展信息 
    private Map<String, Object> extraInfo;


}

