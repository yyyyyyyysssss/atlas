package com.atlas.notification.domain.entity;

import com.atlas.common.core.api.notification.enums.TargetType;
import com.atlas.common.mybatis.entity.BaseIdEntity;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;

/**
 * (NtfNotificationReceiver)实体类
 *
 * @author ys
 * @since 2026-04-15 14:51:18
 */
@Getter
@Setter
@TableName(value = "ntf_notification_receiver", autoResultMap = true)
@Builder
public class NotificationReceiver extends BaseIdEntity {

    @Tolerate
    public NotificationReceiver() {
    }

    // 关联主表 ntf_notification.id 
    @TableField("notification_id")
    private Long notificationId;

    // 接收人标识 
    @TableField("receiver_id")
    private Long receiverId;

    // 实际触达地址: 具体的手机号/邮箱/ID字符串 
    @TableField("receiver_account")
    private String receiverAccount;

    // 接收人目标类型: ALL, USER_ID, EMAIL, PHONE 
    @TableField("target_type")
    @EnumValue
    private TargetType targetType;

    // 读取时间 
    @TableField("read_time")
    private LocalDateTime readTime;

    // false:未读, true:已读
    @TableField("is_read")
    private Boolean isRead;

    @TableField("receive_time")
    private LocalDateTime receiveTime;


}

