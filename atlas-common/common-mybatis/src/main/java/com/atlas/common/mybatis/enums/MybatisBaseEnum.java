package com.atlas.common.mybatis.enums;

import com.atlas.common.core.enums.BaseEnum;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface MybatisBaseEnum<T extends Serializable> extends BaseEnum<T>,IEnum<T> {

    @Override
    default T getValue() {
        return getCode();
    }

    /**
     * 统一 Jackson 序列化：直接返回 code
     */
    @JsonValue
    default T getJsonValue() {
        return getCode();
    }

}
