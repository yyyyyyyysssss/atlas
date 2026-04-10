package com.atlas.common.core.api.notification.builder;

import com.atlas.common.core.api.notification.constant.NotificationConstant;
import com.atlas.common.core.api.notification.enums.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/6 15:42
 */
@Getter
public class NotificationDTO {


    NotificationDTO(String templateCode, String title, NotificationCategory category, Object content, ContentType contentType,
                    List<String> targets, TargetType targetType, List<ChannelType> channels,boolean record,
                    Map<String, Object> params, Map<String, Object> ext) {
        this.templateCode = templateCode;
        this.title = title;
        this.category = category;
        this.content = content;
        this.contentType = contentType;
        this.targets = targets;
        this.targetType = targetType;
        this.channels = channels;
        this.record = record;
        this.params = params;
        this.ext = ext;
    }

    @JsonCreator
    private static NotificationDTO jacksonInstance(
            @JsonProperty("templateCode") String templateCode,
            @JsonProperty("title") String title,
            @JsonProperty("category") NotificationCategory category,
            @JsonProperty("content") Object content,
            @JsonProperty("contentType") ContentType contentType,
            @JsonProperty("targets") List<String> targets,
            @JsonProperty("targetType") TargetType targetType,
            @JsonProperty("channels") List<ChannelType> channels,
            @JsonProperty("record") boolean record,
            @JsonProperty("params") Map<String, Object> params,
            @JsonProperty("ext") Map<String, Object> ext) {
        return new NotificationDTO(templateCode, title, category, content, contentType, targets, targetType, channels,record, params, ext);
    }

    // 模板编码
    private final String templateCode;

    // 可选标题
    private final String title;

    private final NotificationCategory category;

    private final Object content;

    private final ContentType contentType;

    // 接收目标
    private final List<String> targets;

    private final TargetType targetType;

    // 发送渠道
    private final List<ChannelType> channels;

    private boolean record = true;

    // 占位符变量
    private final Map<String, Object> params;

    // 扩展参数
    private final Map<String, Object> ext;

    public RenderType renderType() {
        if (ext == null) return null;
        Object val = ext.get(NotificationConstant.Common.RENDER_TYPE);
        if (val instanceof RenderType rt) return rt;
        if (val instanceof String s) return RenderType.valueOf(s);
        return null;
    }

    public String eventName() {
        if (ext == null) return null;
        Object val = ext.get(NotificationConstant.Inbox.EVENT_NAME);
        if (val instanceof String rt) return rt;
        return null;
    }

}
