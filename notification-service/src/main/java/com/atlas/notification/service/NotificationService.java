package com.atlas.notification.service;

import com.atlas.common.core.api.notification.builder.NotificationDTO;
import com.atlas.notification.domain.entity.Notification;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 15:32
 */
public interface NotificationService extends IService<Notification> {


    void send(NotificationDTO ctx);

}
