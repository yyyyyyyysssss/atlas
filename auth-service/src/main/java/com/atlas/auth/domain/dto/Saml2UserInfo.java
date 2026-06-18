package com.atlas.auth.domain.dto;

import com.atlas.common.core.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/12 14:25
 */
@Data
public class Saml2UserInfo implements ThirdPartyUserIdentity{

    private String provider;

    private String sub;

    private String email;

    private Boolean emailVerified;

    private String fullName;

    private String avatar;

    private Map<String, Object> extraInfo;

    public static Saml2UserInfo fromPrincipal(Saml2AuthenticatedPrincipal principal,
                                              Saml2ProviderSettings.Mapping mappings) {
        Saml2UserInfo saml2User = new Saml2UserInfo();

        String subMapping = mappings.sub();
        String sub = (subMapping != null) ? principal.getFirstAttribute(subMapping) : principal.getName();
        saml2User.setSub(sub);
        // 获取 email
        saml2User.setEmail(principal.getFirstAttribute(mappings.email()));
        // 获取 emailVerified
        Object verifiedObj = principal.getFirstAttribute(mappings.emailVerified());
        saml2User.setEmailVerified(verifiedObj != null && Boolean.parseBoolean(verifiedObj.toString()));
        // 获取姓名和头像
        saml2User.setFullName(principal.getFirstAttribute(mappings.fullName()));
        saml2User.setAvatar(principal.getFirstAttribute(mappings.avatar()));

        Map<String, Object> extraInfo = JsonUtils.convert(principal, new TypeReference<>() {});
        saml2User.setExtraInfo(extraInfo);
        return saml2User;
    }

}
