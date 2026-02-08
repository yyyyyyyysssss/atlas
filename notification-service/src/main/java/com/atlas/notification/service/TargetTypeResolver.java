package com.atlas.notification.service;

import com.atlas.common.api.dto.UserDTO;
import com.atlas.common.api.enums.ChannelType;
import com.atlas.common.api.enums.TargetType;
import com.atlas.common.api.exception.NotificationException;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/6 16:28
 */
public class TargetTypeResolver {

    // 映射：渠道 -> 该渠道原生需要的账号类型 (Channel -> TargetType)
    private static final Map<ChannelType, TargetType> CHANNEL_REQUIRED_TYPE_MAP = new EnumMap<>(ChannelType.class);

    // 映射 目标类型 -> 该目标需要的数据
    private static final Map<TargetType, Function<UserDTO, String>> FIELD_GETTER = new EnumMap<>(TargetType.class);

    static {
        // 配置渠道需求
        CHANNEL_REQUIRED_TYPE_MAP.put(ChannelType.EMAIL, TargetType.EMAIL);
        CHANNEL_REQUIRED_TYPE_MAP.put(ChannelType.SMS, TargetType.PHONE);
        CHANNEL_REQUIRED_TYPE_MAP.put(ChannelType.SSE, TargetType.USER_ID);

        FIELD_GETTER.put(TargetType.USER_ID, v -> String.valueOf(v.getId()));
        FIELD_GETTER.put(TargetType.EMAIL, UserDTO::getEmail);
        FIELD_GETTER.put(TargetType.PHONE, UserDTO::getPhone);
    }

    public static TargetType getRequiredType(ChannelType channel) {
        TargetType type = CHANNEL_REQUIRED_TYPE_MAP.get(channel);
        if (type == null) {
            throw new NotificationException("未配置渠道解析规则: " + channel);
        }
        return type;
    }

    public static Function<UserDTO, String> getGetter(TargetType type) {
        Function<UserDTO, String> getter = FIELD_GETTER.get(type);
        if(getter == null){
            throw new NotificationException("未配置目标类型的数据提取规则: " + type);
        }
        return getter;
    }

}
