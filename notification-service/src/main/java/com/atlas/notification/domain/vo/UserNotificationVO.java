package com.atlas.notification.domain.vo;

import com.atlas.notification.domain.entity.NotificationContent;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/15 16:32
 */
@Getter
@Setter
public class UserNotificationVO {

    private Long id;

    private Long notificationId;

    private String title;

    private String category;

    private LocalDateTime sendTime;

    private NotificationContent content;

    private Boolean isRead;

}
