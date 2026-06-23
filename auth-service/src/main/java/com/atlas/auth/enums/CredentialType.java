package com.atlas.auth.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

/**
 * 第一身份认证凭证类型 (Primary Authentication Credential Type)
 * 用于标识和校验用户登录系统的核心通道，解绑时实行“至少保留一种”的铁律风控。
 */
@Getter
public enum CredentialType {

    PASSWORD("PASSWORD", "传统密码"),
    WEBAUTHN("WEBAUTHN", "Passkey生物识别"),
    WEB3("WEB3", "Web3区块链钱包"),
    THIRD_PARTY("THIRD_PARTY", "第三方社交账号");
    ;

    private final String code;
    private final String description;

    // 构造函数
    CredentialType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonCreator
    public static CredentialType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            // 核心：去除空格并转为大写后再匹配
            return CredentialType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // 可选：如果匹配不上，可以返回 null 让后面的 @NotNull 校验去挡住，或者直接抛出友好异常
            throw new IllegalArgumentException("不支持的类型: " + value);
        }
    }

}
