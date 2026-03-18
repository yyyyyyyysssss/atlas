package com.atlas.notification.enums;

import com.atlas.common.mybatis.enums.MybatisBaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActivationStatus implements MybatisBaseEnum {

    ACTIVE("生效"),
    INACTIVE("禁用"),
    ;

    private final String description;

    @Override
    public String getCode() {
        return this.name();
    }
}
