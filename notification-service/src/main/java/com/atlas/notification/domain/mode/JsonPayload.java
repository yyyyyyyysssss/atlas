package com.atlas.notification.domain.mode;

import com.atlas.common.core.api.notification.enums.ContentType;
import com.atlas.common.core.api.notification.enums.RenderType;
import com.atlas.common.core.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
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
        Map<String, Object> content = new HashMap<>();
        content.put("renderType", renderType.name());
        content.put("body", body);
        content.put("extra", extra);
        // 如果是标准 JSON 字符串，可以直接存入
        return JsonUtils.toJson(content);
    }

    @Override
    @JsonIgnore
    public ContentType getContentType() {
        return ContentType.JSON;
    }
}
