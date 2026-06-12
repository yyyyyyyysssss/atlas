package com.atlas.auth.domain.dto;

import java.util.Collections;
import java.util.Map;

public interface ThirdPartyUserIdentity {

    /**
     * 获取外部身份源的用户永久唯一标识
     * <p>例如：OAuth2 的 sub, SAML2 的 NameID/Subject</p>
     */
    String getSub();

    /**
     * 获取用户主邮箱
     */
    String getEmail();

    /**
     * 邮箱是否已验证
     * <p>默认返回 false，如果三方提供商（如 Auth0）明确带了该状态，由实现类覆盖</p>
     */
    default Boolean getEmailVerified() {
        return false;
    }

    /**
     * 获取用户手机号
     * <p>大部分社交登录默认不带，设为默认方法防止子类强行实现</p>
     */
    default String getPhone() {
        return null;
    }

    /**
     * 手机号是否已验证
     */
    default Boolean getPhoneVerified() {
        return false;
    }

    /**
     * 获取用户显示名称（用于新用户注册时的初始昵称或真实姓名）
     */
    String getFullName();

    /**
     * 获取用户头像 URL
     */
    String getAvatar();

    default Map<String, Object> getExtraInfo() {
        return Collections.emptyMap();
    }

}
