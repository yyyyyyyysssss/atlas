package com.atlas.notification.domain.mode;

import com.atlas.notification.enums.DisplayType;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 11:05
 */
@Getter
@Setter
public class MediaPayload extends MessagePayload{

    private String url;

    private String fileName;

    @Override
    protected DisplayType getDisplayType() {
        return DisplayType.MEDIA;
    }

    @Override
    protected void doValidate() {

    }
}
