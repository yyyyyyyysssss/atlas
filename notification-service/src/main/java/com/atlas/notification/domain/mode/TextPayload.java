package com.atlas.notification.domain.mode;

import com.atlas.common.core.api.notification.enums.ContentType;
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
    public String getContent() {

        return this.text;
    }

    @Override
    public ContentType getContentType() {
        return ContentType.TEXT;
    }
}
