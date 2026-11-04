package com.atlas.auth.domain.dto;

import com.atlas.auth.service.provider.CasProviderEngine;
import com.atlas.common.core.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/12 14:25
 */
@Data
public class CasUserInfo implements ThirdPartyUserIdentity{

    private String provider;

    private String sub;

    private String email;

    private Boolean emailVerified;

    private String fullName;

    private String avatar;

    private Map<String, Object> extraInfo;

    public static CasUserInfo fromPrincipal(CasProviderEngine.CasAuthenticationSuccess principal, CasProviderSettings.Mapping mappings) {
        CasUserInfo saml2User = new CasUserInfo();

        saml2User.setSub(principal.getUser());

        Map<String, Object> attributes = principal.getAttributes();
        // 获取 email
        Object emailObject = attributes.get(mappings.email());
        saml2User.setEmail(emailObject != null ? emailObject.toString() : null);

        // 获取 emailVerified
        Object emailVerifiedObject = attributes.get(mappings.emailVerified());
        saml2User.setEmailVerified(emailVerifiedObject != null && Boolean.parseBoolean(emailVerifiedObject.toString()));

        // 获取姓名
        Object fullNameObject = attributes.get(mappings.fullName());
        saml2User.setFullName(fullNameObject != null ? fullNameObject.toString() : null);

        // 获取头像
        Object avatarObject = attributes.get(mappings.avatar());
        saml2User.setAvatar(avatarObject != null ? avatarObject.toString() : null);

        Map<String, Object> extraInfo = JsonUtils.convert(principal, new TypeReference<>() {});
        saml2User.setExtraInfo(extraInfo);
        return saml2User;
    }

}
