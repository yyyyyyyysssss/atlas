package com.atlas.notification.domain.dto;

import com.atlas.common.core.api.notification.enums.ChannelType;
import com.atlas.common.core.api.notification.enums.NotificationCategory;
import com.atlas.notification.enums.ActivationStatus;
import com.atlas.common.core.api.notification.enums.RenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationTemplateCreateDTO {

    // 模板名称
    @NotBlank(message = "模板名称不能为空")
    private String name;

    // 模板编码
    @NotBlank(message = "模板编码不能为空")
    private String code;

    private String extTemplateCode;

    // 标题模板 
    private String title;

    private NotificationCategory category;

    // 正文模板 
    private String content;

    @NotNull(message = "渠道类型不能为空")
    private ChannelType channelType;

    // 模板格式类型TEXT、HTML
    @NotNull(message = "模板渲染类型不能为空")
    private RenderType renderType;

    // 模板状态：ACTIVE、INACTIVE 
    private ActivationStatus status;

    private String remark;


}

