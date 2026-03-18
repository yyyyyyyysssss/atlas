package com.atlas.common.core.jackson;


import com.atlas.common.core.enums.BaseEnum;
import com.atlas.common.core.jackson.deserializer.BaseEnumDeserializer;
import com.atlas.common.core.jackson.serializer.BaseEnumSerializer;
import com.atlas.common.core.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimeZone;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/10 15:28
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfiguration {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 设置时区
        objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        // 忽略未知属性（防止反序列化失败）
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 仅包含非空字段
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 处理 Java 8+ 的时间类型
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        // 处理 LocalDateTime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        // 处理 LocalDate
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
        // 处理 LocalTime
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
        //注册时间模块
        objectMapper.registerModule(javaTimeModule);
        //  处理 Long 类型精度丢失问题
        SimpleModule simpleModule = new SimpleModule();
        // 将 Long 类型序列化为 String
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        // BigInteger，防止大数溢出
        simpleModule.addSerializer(BigInteger.class, ToStringSerializer.instance);
        // BaseEnum
        simpleModule.addSerializer((Class<BaseEnum<?>>) (Class<?>) BaseEnum.class, new BaseEnumSerializer());
        simpleModule.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
                // 判断当前处理的类是否实现了 BaseEnum 接口
                if (BaseEnum.class.isAssignableFrom(type.getRawClass())) {
                    // 构造时传入具体的子类类型，解决“不知道转成哪个枚举”的问题
                    return new BaseEnumDeserializer<>((Class) type.getRawClass());
                }
                return super.modifyEnumDeserializer(config, type, beanDesc, deserializer);
            }
        });

        objectMapper.registerModule(simpleModule);

        // 初始化json工具类
        JsonUtils.init(objectMapper);
        return objectMapper;
    }

}
