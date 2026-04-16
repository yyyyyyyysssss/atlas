package com.atlas.notification.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserNotificationMarkAsReadDTO{

    @NotNull(message = "通知消息id不能为空")
    private Long notificationId;

}

