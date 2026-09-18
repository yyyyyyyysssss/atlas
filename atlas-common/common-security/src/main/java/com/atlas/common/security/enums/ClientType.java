package com.atlas.common.security.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @Description
 * @Author ys
 * @Date 2024/7/16 17:20
 */
public enum ClientType {

    PC, APP, WEB
    ;
    @JsonCreator
    public static ClientType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            // 核心：去除空格并转为大写后再匹配
            return ClientType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // 可选：如果匹配不上，可以返回 null 让后面的 @NotNull 校验去挡住，或者直接抛出友好异常
            throw new IllegalArgumentException("不支持的类型: " + value);
        }
    }

}
