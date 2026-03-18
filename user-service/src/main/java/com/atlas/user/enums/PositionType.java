package com.atlas.user.enums;

import com.atlas.common.mybatis.enums.MybatisBaseEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum PositionType implements MybatisBaseEnum<String> {

    BASE("BASE", "基础通用岗位"),
    ORG("ORG", "组织下特有岗位"),
    ;

    @EnumValue
    private final String code;
    private final String description;

    // 构造函数
    PositionType(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
