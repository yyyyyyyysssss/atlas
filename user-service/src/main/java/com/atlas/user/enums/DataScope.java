package com.atlas.user.enums;

import com.atlas.common.mybatis.enums.MybatisBaseEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum DataScope implements MybatisBaseEnum<Integer> {

    ALL(50, "全部"),
    CUSTOM(40, "自定义"),
    COMPANY(30, "本公司"),
    DEPT(20, "本部门"),
    SELF(10, "本人"),
    ;

    @EnumValue
    private final Integer code;
    private final String description;

    // 构造函数
    DataScope(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

}
