package com.atlas.common.security.password;

import lombok.Builder;
import lombok.Getter;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/18 13:35
 */
@Getter
@Builder
public class PasswordPolicy {

    /**
     * 密码长度
     */
    @Builder.Default
    private int length = 12;

    /**
     * 是否包含大写字母
     */
    @Builder.Default
    private boolean uppercase = true;

    /**
     * 是否包含小写字母
     */
    @Builder.Default
    private boolean lowercase = true;

    /**
     * 是否包含数字
     */
    @Builder.Default
    private boolean digit = true;


    /**
     * 是否包含特殊字符
     */
    @Builder.Default
    private boolean special = true;


    /**
     * 特殊字符集合
     */
    @Builder.Default
    private String specialChars = "!@#$%^&*()-_+=<>?";

}
