package com.atlas.common.core.api.notification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DisplayType {

    TEXT,   // 对应 ${} 占位符
    JSON,
    HTML,   // 对应 Thymeleaf
    CARD,   // 结构化卡片
    MEDIA   // 媒体/文件
    ;
}
