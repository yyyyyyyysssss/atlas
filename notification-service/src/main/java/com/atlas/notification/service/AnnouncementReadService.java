package com.atlas.notification.service;


import com.atlas.notification.domain.entity.AnnouncementRead;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * (AnnouncementRead)表服务接口
 *
 * @author ys
 * @since 2026-03-30 15:00:27
 */
public interface AnnouncementReadService extends IService<AnnouncementRead> {

    Set<Long> getReadAnnouncementIds(Long userId, List<Long> annIds);

    void markAsRead(Long id, Long userId);
}

