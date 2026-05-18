package com.atlas.auth.enums;

import com.atlas.common.mybatis.enums.MybatisBaseEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum IdentifierStatus implements MybatisBaseEnum<String> {

    ACTIVE("ACTIVE", "已生效"),
    DISABLED("DISABLED", "已停用"),
    DELETED("DELETED", "已删除"),

    ;

    @EnumValue
    private final String code;
    private final String description;

    // 构造函数
    IdentifierStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
