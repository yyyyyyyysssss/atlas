package com.atlas.user.enums;

import com.atlas.common.mybatis.enums.BaseEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum OrganizationType implements BaseEnum<String> {

    GROUP("GROUP", "集团"),
    COMPANY("COMPANY", "公司"),
    DEPT("DEPT", "部门"),
    TEAM("TEAM", "小组"),
    ;

    @EnumValue
    private final String code;
    private final String description;

    // 构造函数
    OrganizationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
