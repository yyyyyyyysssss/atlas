package com.atlas.notification.domain.entity;

import com.atlas.common.core.api.notification.enums.ChannelType;
import com.atlas.common.mybatis.entity.BaseEntity;
import com.atlas.notification.enums.ActivationStatus;
import com.atlas.notification.enums.DisplayType;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

/**
 * (MessageTemplate)实体类
 *
 * @author ys
 * @since 2026-01-30 10:26:11
 */
@Getter
@Setter
@TableName(value = "im_message_template", autoResultMap = true)
@Builder
public class MessageTemplate extends BaseEntity {

    @Tolerate
    public MessageTemplate() {
    }

    // 模板名称 
    @TableField("name")
    private String name;

    // 模板编码 
    @TableField("code")
    private String code;

    // 外部服务商模板ID（如短信服务商ID）
    @TableField("ext_template_code")
    private String extTemplateCode;

    // 标题模板 
    @TableField("title")
    private String title;

    // 正文模板 
    @TableField("content")
    private String content;

    // 模板格式类型TEXT、HTML 
    @TableField("display_type")
    @EnumValue
    private DisplayType displayType;

    // 发送渠道类型
    @TableField("channel_type")
    @EnumValue
    private ChannelType channelType;

    // 模板状态：ACTIVE、INACTIVE 
    @TableField("status")
    private ActivationStatus status;

    // 备注
    @TableField("remark")
    private String remark;


}

