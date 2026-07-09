package com.atlas.auth.enums;

import com.atlas.common.mybatis.enums.MybatisBaseEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum ProjectStatus implements MybatisBaseEnum<String> {


    ACTIVE("active", "启用"),
    SUSPENDED("suspended", "暂停"),
    ARCHIVED("archived", "归档"),

    ;

    @EnumValue
    private final String code;
    private final String description;

    // 构造函数
    ProjectStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonCreator
    public static ProjectStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            // 核心：去除空格并转为大写后再匹配
            return ProjectStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // 可选：如果匹配不上，可以返回 null 让后面的 @NotNull 校验去挡住，或者直接抛出友好异常
            throw new IllegalArgumentException("不支持的类型: " + value);
        }
    }

}
