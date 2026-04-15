package com.atlas.notification.domain.mode;

import com.atlas.common.core.api.notification.enums.ContentType;
import com.atlas.common.core.api.notification.enums.NotificationCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 9:33
 */
@Getter
@Setter
public abstract class MessagePayload {

    private Long notificationId;

    protected String title;

    private NotificationCategory category;

    private LocalDateTime sendTime;

    // 外部服务商模板编码
    @JsonIgnore
    protected String extTemplateCode;

    // 渲染时使用的原始占位符变量
    @JsonIgnore
    protected Map<String, Object> params = new HashMap<>();

    // 扩展字段
    @JsonIgnore
    private Map<String, Object> ext = new HashMap<>();

    public void validate() {
        if (StringUtils.isNotEmpty(title) && title.length() > 100) {
            throw new IllegalArgumentException("消息标题过长");
        }
    }

    public abstract String getContent();

    public abstract ContentType getContentType();

}
