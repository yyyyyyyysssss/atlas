package com.atlas.auth.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VerificationScene {

    LOGIN("login", "登录验证码"),
    REGISTER("register", "注册验证码"),
    RESET_PASSWORD("reset_pwd", "重置密码验证码"),
    MODIFY_EMAIL("modify_email", "修改邮箱验证码");

    private final String code;
    private final String description;

    /**
     * 获取 Redis Key 的一部分或模板 ID
     */
    public String getLowerCaseCode() {
        return code.toLowerCase();
    }

}
