package com.atlas.notification.domain.mode;

import com.atlas.common.core.api.notification.enums.DisplayType;
import com.atlas.common.core.utils.JsonUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
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

    private String btnText;

    // 动态配置 (用于非标准卡片)
    private Map<String, Object> cardConfig;

    @Override
    protected DisplayType getDisplayType() {
        return DisplayType.CARD;
    }

    @Override
    protected void doValidate() {

    }

    @Override
    public String getContent() {
        // 显式投影，排除基类的 title(已在主表), params 等
        Map<String, Object> data = new HashMap<>();
        data.put("body", this.body);
        data.put("subTitle", this.subTitle);
        data.put("imageUrl", this.imageUrl);
        data.put("jumpUrl", this.jumpUrl);
        data.put("cardConfig", this.cardConfig);
        data.put("btnText", this.btnText);
        return JsonUtils.toJson(data);
    }
}
