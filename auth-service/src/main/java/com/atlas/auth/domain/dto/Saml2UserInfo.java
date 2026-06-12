package com.atlas.auth.domain.dto;

import lombok.Data;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/12 14:25
 */
@Data
public class Saml2UserInfo implements ThirdPartyUserIdentity{

    private String sub;

    private String email;

    private Boolean emailVerified;

    private String fullName;

    private String avatar;

    private Map<String, Object> extraInfo;

}
