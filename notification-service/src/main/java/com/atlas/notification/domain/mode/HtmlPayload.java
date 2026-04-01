package com.atlas.notification.domain.mode;

import com.atlas.common.core.api.notification.enums.ContentType;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 9:37
 */
@Getter
@Setter
public class HtmlPayload extends MessagePayload{

    private String html;

    @Override
    public String getContent() {

        return this.html;
    }

    @Override
    public ContentType getContentType() {
        return ContentType.HTML;
    }
}
