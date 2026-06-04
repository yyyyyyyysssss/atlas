package com.atlas.auth.enums;


import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SecurityScene {

    LOGIN("login", "登录"),
    REGISTER("register", "注册"),
    RESET_PASSWORD("reset_pwd", "重置密码"),
    MODIFY_EMAIL("modify_email", "修改邮箱"),
    UNBIND_WEBAUTHN("unbind_webauthn", "解绑通行密钥"),
    UNBIND_TOTP("unbind_totp", "解绑TOTP"),
    GENERATE_TOTP_BACKUP_CODE("generate_totp_backup_code", "重新生成TOTP备份码"),
    BIND_GESTURE("bind_gesture", "绑定手势密码"),
    UNBIND_GESTURE("unbind_gesture", "解绑手势密码"),
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
    public static SecurityScene fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            // 核心：去除空格并转为大写后再匹配
            return SecurityScene.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // 可选：如果匹配不上，可以返回 null 让后面的 @NotNull 校验去挡住，或者直接抛出友好异常
            throw new IllegalArgumentException("不支持的验证码场景: " + value);
        }
    }

}
