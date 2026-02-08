package com.atlas.notification.domain.mode;

import com.atlas.notification.enums.DisplayType;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

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
    protected DisplayType getDisplayType() {
        return DisplayType.HTML;
    }

    @Override
    protected void doValidate() {
        if (StringUtils.isBlank(html)) {
            throw new IllegalArgumentException("HTML内容不能为空");
        }
    }
}
