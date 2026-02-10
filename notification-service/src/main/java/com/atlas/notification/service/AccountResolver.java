package com.atlas.notification.service;


import com.atlas.common.core.api.user.UserApi;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.api.notification.enums.ChannelType;
import com.atlas.common.core.api.notification.enums.TargetType;
import com.atlas.common.core.api.notification.exception.NotificationException;
import com.atlas.common.core.response.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/9 10:32
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountResolver {

    private final UserApi userApi;

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

    public List<String> resolve(ChannelType channel, TargetType targetType, List<String> targets) {
        if (CollectionUtils.isEmpty(targets)) {
            return Collections.emptyList();
        }
        // 渠道需要的目标类型
        TargetType requiredType = getRequiredType(channel);
        // 匹配则直发
        if (targetType == requiredType) {
            return targets;
        }

        List<UserDTO> users = switch (targetType) {
            case USER_ID -> Optional.ofNullable(userApi.findByIds(targets.stream().map(Long::valueOf).toList())).filter(Result::isSucceed).orElseThrow().getData();
            case EMAIL   -> Optional.ofNullable(userApi.findByEmails(targets)).filter(Result::isSucceed).orElseThrow().getData();
            case PHONE   -> Optional.ofNullable(userApi.findByPhones(targets)).filter(Result::isSucceed).orElseThrow().getData();
        };
        Function<UserDTO, String> getter = getGetter(requiredType);
        return users.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
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
