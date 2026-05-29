package com.atlas.auth.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CaptchaScene {

    LOGIN("login", "登录验证码"),
    REGISTER("register", "注册验证码"),
    RESET_PASSWORD("reset_pwd", "重置密码验证码"),
    MODIFY_EMAIL("modify_email", "修改邮箱验证码"),
    UNBIND_WEBAUTHN("unbind_webauthn", "解绑通行密钥验证码"),
    UNBIND_TOTP("unbind_totp", "解绑TOTP验证码"),
    GENERATE_TOTP_BACKUP_CODE("generate_totp_backup_code", "重新生成TOTP备份码验证码"),

    DEFAULT("default", "通用安全验证码"),
    ;

    private final String code;
    private final String description;

    /**
     * 获取 Redis Key 的一部分或模板 ID
     */
    public String getLowerCaseCode() {
        return code.toLowerCase();
    }

    @JsonCreator
    public static CaptchaScene fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            // 核心：去除空格并转为大写后再匹配
            return CaptchaScene.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // 可选：如果匹配不上，可以返回 null 让后面的 @NotNull 校验去挡住，或者直接抛出友好异常
            throw new IllegalArgumentException("不支持的验证码场景: " + value);
        }
    }

}
