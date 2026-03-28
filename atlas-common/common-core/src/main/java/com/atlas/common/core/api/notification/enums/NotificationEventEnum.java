package com.atlas.common.core.api.notification.enums;


import com.atlas.common.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationEventEnum implements BaseEnum<String> {

    CONNECTED("connected", "连接事件"),
    HEARTBEAT("heartbeat", "心跳事件"),


    ANNOUNCEMENT_EVENT("announcement_event", "系统公告"),

    ;

    private final String code;
    private final String description;

}
