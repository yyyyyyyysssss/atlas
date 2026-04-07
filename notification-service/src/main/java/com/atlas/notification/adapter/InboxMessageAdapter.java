package com.atlas.notification.adapter;


import com.atlas.common.core.api.notification.constant.NotificationConstant;
import com.atlas.common.core.api.notification.enums.ChannelType;
import com.atlas.common.core.api.notification.enums.ContentType;
import com.atlas.common.core.api.notification.enums.NotificationEventEnum;
import com.atlas.common.core.enums.BaseEnum;
import com.atlas.notification.domain.mode.MessagePayload;
import com.atlas.notification.sse.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 15:30
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InboxMessageAdapter extends AbstractMessageAdapter implements MessageAdapter {

    private final NotificationPublisher notificationPublisher;

    @Override
    public boolean support(ChannelType channelType) {
        return channelType == ChannelType.INBOX;
    }

    @Override
    public void send(MessagePayload payload, List<String> targets) {
        // 统一序列化为字符串
        String content = payload.getContent();
        ContentType contentType = payload.getContentType();
        // 提取事件名称
        Map<String, Object> ext = Optional.ofNullable(payload.getExt()).orElse(Collections.emptyMap());

        String eventName = getAsString(ext, NotificationConstant.Inbox.EVENT_NAME);

        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        data.put("contentType", contentType.name());
        for (String target : targets) {
            try {
                Long userId = Long.valueOf(target);
                notificationPublisher.publish(userId, BaseEnum.fromCode(NotificationEventEnum.class,eventName),data);
            } catch (Exception e) {
                log.error("SSE publish failed for user: {}, error: {}", target, e.getMessage());
            }

        }
    }
}
