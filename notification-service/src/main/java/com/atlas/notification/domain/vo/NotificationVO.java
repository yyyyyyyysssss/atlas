package com.atlas.notification.domain.vo;

import com.atlas.common.core.api.notification.enums.ChannelType;
import com.atlas.common.core.api.notification.enums.NotificationCategory;
import com.atlas.notification.enums.NotificationStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class NotificationVO {


    private Long id;

    // 标题 
    private String title;

    // 渠道类型: EMAIL, SMS, INBOX 
    private ChannelType channelType;

    // 关联的模板编码 
    private String templateCode;

    // 消息正文 
    private String content;

    // SENDING: 发送中, SENT: 已发出 FAILED: 发送失败 
    private NotificationStatus status;

    private NotificationCategory category;

    // 发送时间 
    private String sendTime;

    // 原始模板参数快照 
    private Map<String,Object> params;

    // 扩展配置 
    private Map<String,Object> ext;

    private Long creatorId;

    private String creatorName;

    private String createTime;

    private Long updaterId;

    private String updaterName;

    private String updateTime;


    public String getStatusName(){

        return status != null ? status.getDescription() : null;
    }
}

