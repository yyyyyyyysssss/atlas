package com.atlas.common.core.jackson.deserializer;

import com.atlas.common.core.enums.BaseEnum;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;
import java.io.Serializable;

/**
 * @Description
 * @Author ys
 * @Date 2026/3/17 14:09
 */
public class BaseEnumDeserializer<E extends Enum<E> & BaseEnum<V>, V extends Serializable> extends JsonDeserializer<E> implements ContextualDeserializer {

    private Class<E> enumClass;

    public BaseEnumDeserializer() {}

    public BaseEnumDeserializer(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public E deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        // 获取 JSON 中的原始值（可能是数字，也可能是字符串 "50"）
        Object value = jsonParser.readValueAs(Object.class);
        if (value == null) {
            return null;
        }
        return (E) BaseEnum.fromCode((Class) enumClass, (Serializable) value);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext deserializationContext, BeanProperty beanProperty) {
        Class<?> rawClass = deserializationContext.getContextualType().getRawClass();
        return new BaseEnumDeserializer(rawClass);
    }
}
