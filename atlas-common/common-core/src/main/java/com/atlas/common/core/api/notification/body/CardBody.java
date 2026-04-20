package com.atlas.common.core.api.notification.body;

import com.atlas.common.core.api.notification.enums.RenderType;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/2 10:18
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardBody implements NotificationBody {

    private String subTitle;
    private String content;
    private String imageUrl;
    private String link;

    @Singular
    private List<KVField> fields; // 关键业务字段列表（Key-Value对）
    private String tagText;       // 卡片右上角的标签文字（如：待审批）

    @Builder.Default
    private TargetType tagType = TargetType.DEFAULT;       // 标签颜色类型（success, warning, error）

    @Singular
    private List<Action> actions;

    @Override
    public RenderType getRenderType() {
        return RenderType.CARD;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Action {
        private String label;

        /**
         * 动作目标路径
         * URL模式: https://...
         * ROUTE/DRAWER模式: /system/user/profile
         * API模式: /api/v1/order/confirm
         */
        private String path;

        /**
         * 动作执行类型
         * URL: 外部浏览器打开
         * ROUTE: SPA 页面内跳转
         * DRAWER: 弹出宽抽屉并在其中渲染路由组件 (你现在的核心需求)
         * API: 静默发送一个异步请求（如“一键签收”）
         */
        @Builder.Default
        private ActionType actionType = ActionType.ROUTE;

        // 样式类型: primary, danger, default, dashed, link
        @Builder.Default
        private ActionTheme theme = ActionTheme.DEFAULT;

        /**
         * 交互反馈：如果配置了此项，前端点击后会先弹出二次确认框
         * 例如："确定要删除该备份吗？"
         */
        private String confirmText;

        /**
         * 打开位置：_blank (新窗口), _self (当前窗口)
         */
        @Builder.Default
        private ActionTarget target = ActionTarget._BLANK;

        /**
         * 扩展参数：
         * API模式下可放置 { "method": "POST" }
         * ROUTE模式下可放置路由 state 数据
         */
        @Singular("extraInfo")
        private Map<String, Object> extra;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KVField {
        private String label;
        private String value;
        @Builder.Default
        private Boolean highlight = Boolean.FALSE; // 是否高亮显示值
    }

    public enum TargetType {
        SUCCESS,
        WARNING,
        ERROR,
        DEFAULT,
        PROCESSING,
    }

    public enum ActionType {
        URL,
        ROUTE,
        DRAWER,
        API
    }

    public enum ActionTheme {
        PRIMARY,
        DANGER,
        DEFAULT,
        DASHED,
        LINK
    }

    public enum ActionTarget {
        _BLANK,
        _SELF,
    }
}
