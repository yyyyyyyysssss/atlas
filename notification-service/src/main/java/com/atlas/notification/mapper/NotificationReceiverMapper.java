package com.atlas.notification.mapper;

import com.atlas.notification.domain.entity.NotificationReceiver;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (NtfNotificationReceiver)表数据库访问层
 *
 * @author ys
 * @since 2026-04-15 14:51:18
 */
@Mapper
public interface NotificationReceiverMapper extends BaseMapper<NotificationReceiver> {
    
}

