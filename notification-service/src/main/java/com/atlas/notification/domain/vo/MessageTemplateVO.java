package com.atlas.notification.domain.vo;

import com.atlas.common.api.enums.ChannelType;
import com.atlas.notification.enums.ActivationStatus;
import com.atlas.notification.enums.DisplayType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageTemplateVO {


    private Long id;

    // 模板名称 
    private String name;

    // 模板编码 
    private String code;

    // 外部服务商模板ID
    private String extTemplateCode;

    // 标题模板 
    private String title;

    // 正文模板 
    private String content;

    // 模板格式类型TEXT、HTML 
    private DisplayType displayType;

    // 渠道类型
    private ChannelType channelType;

    // 模板状态：ACTIVE、INACTIVE 
    private ActivationStatus status;

    // 备注
    private String remark;

    // 创建时间 
    private String createTime;

    // 创建人ID 
    private Long creatorId;

    // 创建人名称 
    private String creatorName;

    // 更新时间 
    private String updateTime;

    // 更新人ID 
    private Long updaterId;

    // 更新人名称 
    private String updaterName;

    public String getStatusDescription() {
        return status != null ? status.getDescription() : null;
    }

}

