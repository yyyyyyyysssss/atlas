package com.atlas.user.enums;

import com.atlas.common.mybatis.enums.MybatisBaseEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum AuthorityDomain implements MybatisBaseEnum<String> {


    GLOBAL("GLOBAL", "全局"),
    PROJECT("PROJECT", "项目"),
    ;

    @EnumValue
    private final String code;
    private final String description;

    // 构造函数
    AuthorityDomain(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonCreator
    public static AuthorityDomain fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return AuthorityDomain.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AuthorityDomain.GLOBAL;
        }
    }


}
