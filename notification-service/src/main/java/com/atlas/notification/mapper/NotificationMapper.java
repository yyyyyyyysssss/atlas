package com.atlas.notification.mapper;

import com.atlas.notification.domain.entity.Notification;
import com.atlas.notification.domain.vo.UserNotificationVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * (NtfNotification)表数据库访问层
 *
 * @author ys
 * @since 2026-04-01 09:44:19
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    List<UserNotificationVO> selectUserNotifications(@Param("userId") Long userId);
    
}

