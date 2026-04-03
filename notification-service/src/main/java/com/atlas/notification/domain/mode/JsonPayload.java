package com.atlas.notification.domain.mode;

import com.atlas.common.core.api.notification.enums.ContentType;
import com.atlas.common.core.api.notification.enums.RenderType;
import com.atlas.common.core.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class JsonPayload extends MessagePayload{

    private RenderType renderType;

    private Object body;

    private Map<String, Object> extra;

    @Override
    @JsonIgnore
    public String getContent() {
        // 如果是标准 JSON 字符串，可以直接存入
        return JsonUtils.toJson(this);
    }

    @Override
    @JsonIgnore
    public ContentType getContentType() {
        return ContentType.JSON;
    }
}
