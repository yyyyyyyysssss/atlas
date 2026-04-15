package com.atlas.notification.service.impl;

import com.atlas.notification.domain.entity.NotificationReceiver;
import com.atlas.notification.mapper.NotificationReceiverMapper;
import com.atlas.notification.service.NotificationReceiverService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * (NtfNotificationReceiver)表服务实现类
 *
 * @author ys
 * @since 2026-04-15 14:51:18
 */
@Service("ntfNotificationReceiverService")
@AllArgsConstructor
@Slf4j
public class NotificationReceiverServiceImpl extends ServiceImpl<NotificationReceiverMapper, NotificationReceiver> implements NotificationReceiverService {
    
    private NotificationReceiverMapper notificationReceiverMapper;
    

    
}

