package com.atlas.auth.enums;

import com.atlas.common.mybatis.enums.MybatisBaseEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum IdentifierType implements MybatisBaseEnum<String> {

    USERNAME("USERNAME", "账号"),
    EMAIL("EMAIL", "邮箱"),
    PHONE("PHONE", "手机号"),

    ;

    @EnumValue
    private final String code;
    private final String description;

    // 构造函数
    IdentifierType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
