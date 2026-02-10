package com.atlas.common.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2024/4/29 13:41
 */
@Slf4j
public class JsonUtils {


    private static volatile ObjectMapper mapper;

    public static void init(ObjectMapper objectMapper) {
        JsonUtils.mapper = objectMapper;
    }

    public static ObjectMapper getMapper() {
        if (mapper == null) {
            // 兜底方案：防止某些极端场景下 Spring 还没启动完成就调用工具类
            synchronized (JsonUtils.class) {
                if (mapper == null) mapper = new ObjectMapper();
            }
        }
        return mapper;
    }

    public static String toJson(Object object){
        try {
            return getMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String s,Class<T> tClass){
        try {
            return getMapper().readValue(s,tClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(byte[] bytes,Class<T> tClass){
        try {
            return getMapper().readValue(bytes,tClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String s, TypeReference<T> typeReference){
        try {
            return getMapper().readValue(s,typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> parseArray(String s,Class<T> tClass){
        CollectionType collectionType = getMapper().getTypeFactory().constructCollectionType(List.class, tClass);
        try {
            return getMapper().readValue(s,collectionType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> parseArray(byte[] bytes,Class<T> tClass){
        CollectionType collectionType = getMapper().getTypeFactory().constructCollectionType(List.class, tClass);
        try {
            return getMapper().readValue(bytes,collectionType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
