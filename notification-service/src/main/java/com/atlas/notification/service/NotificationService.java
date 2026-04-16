package com.atlas.notification.service;

import com.atlas.common.core.api.notification.builder.NotificationDTO;
import com.atlas.notification.domain.entity.Notification;
import com.atlas.notification.domain.vo.UserNotificationVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 15:32
 */
public interface NotificationService extends IService<Notification> {


    void send(NotificationDTO ctx);

    Integer countUnread(Long userId);

    PageInfo<UserNotificationVO> userNotificationList(Long userId, Integer pageNum, Integer pageSize);

    void markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);

}
