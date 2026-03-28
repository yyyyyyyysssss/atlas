package com.atlas.notification.sse;

import com.atlas.common.core.api.notification.enums.NotificationEventEnum;

public interface NotificationSubscriber {

    /**
     * 处理消息推送
     * @param userId    目标用户ID (null 表示广播)
     * @param eventName 事件名 (如: "order_pay", "sys_notice")
     * @param data      消息载体
     */
    void onMessage(Long userId, NotificationEventEnum eventName, Object data);

}
