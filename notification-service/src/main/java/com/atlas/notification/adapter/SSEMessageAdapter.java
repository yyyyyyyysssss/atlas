package com.atlas.notification.adapter;


import com.atlas.common.core.api.notification.constant.NotificationConstant;
import com.atlas.common.core.api.notification.enums.ChannelType;
import com.atlas.common.core.api.notification.enums.NotificationEventEnum;
import com.atlas.common.core.enums.BaseEnum;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.notification.domain.mode.CardPayload;
import com.atlas.notification.domain.mode.JsonPayload;
import com.atlas.notification.domain.mode.MessagePayload;
import com.atlas.notification.domain.mode.TextPayload;
import com.atlas.notification.sse.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 15:30
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SSEMessageAdapter extends AbstractMessageAdapter implements MessageAdapter {

    private final NotificationPublisher notificationPublisher;

    @Override
    public boolean support(ChannelType channelType) {
        return channelType == ChannelType.SSE;
    }

    @Override
    public void send(MessagePayload payload, List<String> targets) {
        Object sendContent;
        if (payload instanceof JsonPayload jsonPayload) {
            sendContent = jsonPayload.getData();
        } else if (payload instanceof TextPayload textPayload) {
            sendContent = textPayload.getText();
        } else if (payload instanceof CardPayload cardPayload) {
            // 如果是卡片，通常也是发整个卡片对象给前端渲染
            sendContent = cardPayload;
        } else {
            log.warn("Unsupported payload type for SSE: {}", payload.getClass().getSimpleName());
            return;
        }
        // 统一序列化为字符串
        String dataString = (sendContent instanceof String str)
                ? str
                : JsonUtils.toJson(sendContent);
        // 提取事件名称
        Map<String, Object> ext = Optional.ofNullable(payload.getExt()).orElse(Collections.emptyMap());

        String eventName = getAsString(ext, NotificationConstant.Sse.EVENT_NAME);

        for (String target : targets) {
            try {
                Long userId = Long.valueOf(target);
                notificationPublisher.publish(userId, BaseEnum.fromCode(NotificationEventEnum.class,eventName),dataString);
            } catch (Exception e) {
                log.error("SSE publish failed for user: {}, error: {}", target, e.getMessage());
            }

        }
    }
}
