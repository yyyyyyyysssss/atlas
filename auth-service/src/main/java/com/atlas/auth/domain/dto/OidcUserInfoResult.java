package com.atlas.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/23 13:53
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OidcUserInfoResult implements ThirdPartyUserIdentity{

    private String sub;

    private String provider;

    private String name;

    @JsonProperty("preferred_username")
    private String preferredUsername;

    private String picture;

    /** 邮箱 (可选) */
    private String email;

    @JsonProperty("email_verified")
    private Boolean emailVerified;

    /** 手机号 (可选) */
    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("phone_number_verified")
    private Boolean phoneNumberVerified;

    private Map<String, Object> extraInfo;

    @Override
    public String getPhone() {
        return this.phoneNumber;
    }

    @Override
    public Boolean getPhoneVerified() {
        return this.phoneNumberVerified;
    }

    @Override
    public String getFullName() {
        return this.name;
    }

    @Override
    public String getAvatar() {
        return this.picture;
    }
}
