package com.atlas.notification.domain.mode;

import com.atlas.notification.enums.DisplayType;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 11:05
 */
@Getter
@Setter
public class CardPayload extends MessagePayload{

    private String body;

    private String subTitle;

    private String imageUrl;

    private String jumpUrl;

    // 动态配置 (用于非标准卡片)
    private Map<String, Object> cardConfig;

    @Override
    protected DisplayType getDisplayType() {
        return DisplayType.CARD;
    }

    @Override
    protected void doValidate() {

    }
}
