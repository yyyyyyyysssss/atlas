package com.atlas.common.mybatis.handler;

import com.atlas.common.mybatis.enums.BaseEnum;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;
import java.io.Serializable;

public class BaseEnumDeserializer extends JsonDeserializer<BaseEnum<?>> implements ContextualDeserializer {

    private Class<? extends BaseEnum<?>> enumClass;

    public BaseEnumDeserializer() {}

    public BaseEnumDeserializer(Class<? extends BaseEnum<?>> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        JavaType type = (property != null) ? property.getType() : ctxt.getContextualType();
        if (type == null) {
            return this;
        }
        Class<?> rawClass = type.getRawClass();
        // 2. 只有符合条件的才创建新的实例
        if (Enum.class.isAssignableFrom(rawClass) && BaseEnum.class.isAssignableFrom(rawClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends BaseEnum<?>> targetEnumClass = (Class<? extends BaseEnum<?>>) rawClass;
            return new BaseEnumDeserializer(targetEnumClass);
        }
        return this;
    }


    @Override
    public BaseEnum<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        Object code = jsonParser.readValueAs(Object.class);
        if (code == null) {
            return null;
        }
        // 2. 检查 enumClass 是否已通过 createContextual 注入
        if (enumClass == null) {
            return null;
        }
        if (!(code instanceof Serializable)) {
            return null;
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        BaseEnum<?> result = (BaseEnum<?>)BaseEnum.fromCode((Class) enumClass, (Serializable)code);
        return result;
    }


}
