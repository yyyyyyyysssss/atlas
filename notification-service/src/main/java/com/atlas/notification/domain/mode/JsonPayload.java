package com.atlas.notification.domain.mode;

import com.atlas.common.core.api.notification.enums.DisplayType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JsonPayload extends MessagePayload{

    private String json;

    @Override
    protected DisplayType getDisplayType() {
        return DisplayType.JSON;
    }

    @Override
    protected void doValidate() {

    }

    @Override
    public String getContent() {
        // 如果是标准 JSON 字符串，可以直接存入
        return this.json;
    }
}
