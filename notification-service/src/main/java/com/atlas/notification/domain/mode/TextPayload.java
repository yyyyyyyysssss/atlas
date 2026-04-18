package com.atlas.notification.domain.mode;

import com.atlas.common.core.api.notification.enums.RenderType;
import com.atlas.notification.domain.entity.NotificationContent;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 9:35
 */
@Getter
@Setter
public class TextPayload extends MessagePayload{

    private String text;

    @Override
    public NotificationContent getPayloadContent() {
        // 即使是文本，也包装成统一格式存入 content 字段
        return NotificationContent.builder()
                .renderType(RenderType.TEXT)
                .body(this.text)
                .build();
    }
}
