package com.atlas.notification.domain.mode;

import com.atlas.common.core.api.notification.enums.DisplayType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JsonPayload extends MessagePayload{

    private Object data;

    @Override
    protected DisplayType getDisplayType() {
        return DisplayType.JSON;
    }

    @Override
    protected void doValidate() {

    }
}
