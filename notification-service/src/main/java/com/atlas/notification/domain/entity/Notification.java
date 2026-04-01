package com.atlas.notification.domain.entity;

import com.atlas.common.core.api.notification.enums.ChannelType;
import com.atlas.common.core.api.notification.enums.ContentType;
import com.atlas.common.mybatis.entity.BaseEntity;
import com.atlas.notification.enums.NotificationStatus;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * (NtfNotification)实体类
 *
 * @author ys
 * @since 2026-04-01 09:44:19
 */
@Getter
@Setter
@TableName(value = "ntf_notification", autoResultMap = true)
@Builder
public class Notification extends BaseEntity {

    @Tolerate
    public Notification() {
    }

    // 标题 
    @TableField("title")
    private String title;

    // 渠道类型: EMAIL, SMS, INBOX 
    @TableField("channel_type")
    @EnumValue
    private ChannelType channelType;

    // 关联的模板编码 
    @TableField("template_code")
    private String templateCode;

    // 消息正文 
    @TableField("content")
    private String content;

    @TableField("content_type")
    @EnumValue
    private ContentType contentType;

    // SENDING: 发送中, SENT: 已发出 FAILED: 发送失败 
    @TableField("status")
    private NotificationStatus status;

    // 发送时间 
    @TableField("send_time")
    private LocalDateTime sendTime;

    @TableField("fail_reason")
    private String failReason;

    // 原始模板参数快照 
    @TableField(value = "params", typeHandler = JacksonTypeHandler.class)
    private Map<String,Object> params;

    // 扩展配置 
    @TableField(value = "ext", typeHandler = JacksonTypeHandler.class)
    private Map<String,Object> ext;


}

