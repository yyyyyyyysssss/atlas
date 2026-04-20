package com.atlas.notification.sse;

import lombok.Data;

import java.io.Serializable;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/20 13:54
 */
@Data
public class NotificationStreamMessage implements Serializable {

    private Long userId;

    private String eventCode;

    private Object data;

    public static NotificationStreamMessage of(Long userId, String eventCode, Object data) {
        NotificationStreamMessage notificationStreamMessage = new NotificationStreamMessage();
        notificationStreamMessage.setUserId(userId);
        notificationStreamMessage.setEventCode(eventCode);
        notificationStreamMessage.setData(data);
        return notificationStreamMessage;
    }
}
