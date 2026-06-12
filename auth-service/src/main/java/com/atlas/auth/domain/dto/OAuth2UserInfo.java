package com.atlas.auth.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/8 16:13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2UserInfo implements ThirdPartyUserIdentity{

    /** 唯一标识 (授权服务器返回的 sub) */
    private String sub;

    /** 平台标识 (如: atlas, google, github) */
    private String provider;

    /** 用户昵称/姓名 (从 id_token 中解析) */
    private String fullName;

    /** 头像地址 */
    private String avatar;

    /** 邮箱 (可选) */
    private String email;

    private Boolean emailVerified;

    /** 手机号 (可选) */
    private String phone;

    private Boolean phoneVerified;

    private Map<String, Object> extraInfo;

}
