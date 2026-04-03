package com.atlas.common.core.api.notification.body;

import com.atlas.common.core.api.notification.enums.RenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/2 10:18
 */
@Getter
@Builder
public class CardBody implements NotificationBody {

    private String subTitle;
    private String content;
    private String imageUrl;
    private String link;

    private List<KVField> fields; // 关键业务字段列表（Key-Value对）
    private String tagText;       // 卡片右上角的标签文字（如：待审批）
    private String tagType;       // 标签颜色类型（success, warning, error）

    private List<Button> actions;

    @Override
    public RenderType getRenderType() {
        return RenderType.CARD;
    }

    @Data
    @AllArgsConstructor
    public static class Button {
        private String label;
        private String url;
        @Builder.Default
        private String type = "default"; // primary, danger, default
        @Builder.Default
        private String actionType = "URL"; // URL(跳转), API(异步请求), JS(自定义脚本)
    }

    @Data
    @AllArgsConstructor
    public static class KVField {
        private String label;
        private String value;
        private Boolean highlight; // 是否高亮显示值
    }
}
