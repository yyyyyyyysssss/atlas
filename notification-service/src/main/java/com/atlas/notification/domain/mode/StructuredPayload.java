package com.atlas.notification.domain.mode;

import com.atlas.common.core.api.notification.enums.RenderType;
import com.atlas.notification.domain.entity.NotificationContent;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class StructuredPayload extends MessagePayload{

    private RenderType renderType;

    private Object body;

    private Map<String, Object> extra;


    @Override
    public NotificationContent getPayloadContent() {
        return NotificationContent.builder()
                .renderType(renderType)
                .body(this.body)
                .extra(this.extra)
                .build();
    }
}
