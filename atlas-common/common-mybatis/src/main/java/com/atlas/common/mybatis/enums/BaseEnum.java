package com.atlas.common.mybatis.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public interface BaseEnum<T extends Serializable> extends IEnum<T> {

    /**
     * 获取枚举的显示名称（通常用于前端展示或日志）
     */
    String getDescription();

    /**
     * 获取枚举的值（对应数据库存储的值）
     */
    T getCode();

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

    Map<Class<?>, Map<String, BaseEnum<?>>> ENUM_CACHE = new ConcurrentHashMap<>();

    /**
     * 静态工具方法（供业务代码或自定义反序列化器使用）
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> E fromCode(Class<E> enumClass, V code) {
        if (code == null || enumClass == null) {
            return null;
        }
        // 预先转为字符串，消除 Integer/Long/String 的类型差异
        String targetCode = String.valueOf(code);
        Map<String, BaseEnum<?>> map = ENUM_CACHE.computeIfAbsent(enumClass, clazz -> {
            Map<String, BaseEnum<?>> internalMap = new HashMap<>();
            for (E e : enumClass.getEnumConstants()) {
                internalMap.put(String.valueOf(e.getCode()), e);
            }
            return internalMap;
        });

        return enumClass.cast(map.get(targetCode));
    }

}
