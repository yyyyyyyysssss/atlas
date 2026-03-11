package com.atlas.user.enums;

import com.atlas.common.mybatis.enums.BaseEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/28 17:47
 */
@Getter
public enum PositionStatus implements BaseEnum<String>{

    ACTIVE("ACTIVE", "启用"),
    INACTIVE("INACTIVE", "停用"),
    ;

    @EnumValue
    private final String code;
    private final String description;

    // 构造函数
    PositionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
