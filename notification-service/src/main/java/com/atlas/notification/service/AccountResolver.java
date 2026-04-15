package com.atlas.notification.service;


import com.atlas.common.core.api.notification.enums.ChannelType;
import com.atlas.common.core.api.notification.enums.TargetType;
import com.atlas.common.core.api.notification.exception.NotificationException;
import com.atlas.common.core.api.user.UserApi;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.response.Result;
import com.atlas.notification.domain.mode.ResolvedTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
        CHANNEL_REQUIRED_TYPE_MAP.put(ChannelType.INBOX, TargetType.USER_ID);

        FIELD_GETTER.put(TargetType.USER_ID, v -> String.valueOf(v.getId()));
        FIELD_GETTER.put(TargetType.EMAIL, UserDTO::getEmail);
        FIELD_GETTER.put(TargetType.PHONE, UserDTO::getPhone);
    }

    public List<ResolvedTarget> resolve(ChannelType channel, TargetType targetType, List<String> targets) {
        if (CollectionUtils.isEmpty(targets) && !targetType.equals(TargetType.ALL)) {
            return Collections.emptyList();
        }
        // 渠道需要的目标类型
        TargetType requiredType = getRequiredType(channel);

        List<UserDTO> users = switch (targetType) {
            case ALL -> safeGetData(userApi.findAll());
            case USER_ID -> safeGetData(userApi.findByIdentifier(targets));
            case EMAIL -> safeGetData(userApi.findByEmails(targets));
            case PHONE -> safeGetData(userApi.findByPhones(targets));
        };
        // 转换已存在系统的用户
        Function<UserDTO, String> getter = getGetter(requiredType);
        List<ResolvedTarget> resolvedList = new ArrayList<>(users.stream()
                .map(u -> new ResolvedTarget(u.getId(), getter.apply(u)))
                .filter(rt -> StringUtils.isNotBlank(rt.getAccount()))
                .distinct()
                .toList());
        // 处理匿名/外部目标
        if (targetType == requiredType && !targetType.equals(TargetType.ALL)) {
            Set<String> resolvedAccounts = resolvedList.stream()
                    .map(ResolvedTarget::getAccount)
                    .collect(Collectors.toSet());
            targets.stream()
                    .filter(t -> !resolvedAccounts.contains(t))
                    .forEach(t -> resolvedList.add(new ResolvedTarget(null, t)));
        }

        return resolvedList.stream().distinct().collect(Collectors.toList());
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
        if (getter == null) {
            throw new NotificationException("未配置目标类型的数据提取规则: " + type);
        }
        return getter;
    }

    private List<UserDTO> safeGetData(Result<List<UserDTO>> result) {
        return (result != null && result.isSucceed() && result.getData() != null)
                ? result.getData() : Collections.emptyList();
    }

}
