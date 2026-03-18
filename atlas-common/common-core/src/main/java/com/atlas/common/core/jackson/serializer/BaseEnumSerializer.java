package com.atlas.common.core.jackson.serializer;

import com.atlas.common.core.enums.BaseEnum;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * @Description
 * @Author ys
 * @Date 2026/3/17 14:08
 */
public class BaseEnumSerializer extends JsonSerializer<BaseEnum<?>> {

    @Override
    public void serialize(BaseEnum<?> baseEnum, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (baseEnum == null) {
            jsonGenerator.writeNull();
            return;
        }
        jsonGenerator.writeObject(baseEnum.getCode());
    }
}
