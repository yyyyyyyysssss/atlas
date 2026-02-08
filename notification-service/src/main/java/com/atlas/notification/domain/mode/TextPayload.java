package com.atlas.notification.domain.mode;

import com.atlas.notification.enums.DisplayType;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

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
    protected DisplayType getDisplayType() {
        return DisplayType.TEXT;
    }

    @Override
    protected void doValidate() {
        if (StringUtils.isBlank(text)) {
            throw new IllegalArgumentException("HTML内容不能为空");
        }
    }
}
