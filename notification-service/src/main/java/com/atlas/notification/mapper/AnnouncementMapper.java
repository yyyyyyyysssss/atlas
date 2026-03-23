package com.atlas.notification.mapper;

import com.atlas.notification.domain.entity.Announcement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (Announcement)表数据库访问层
 *
 * @author ys
 * @since 2026-03-23 14:57:28
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
    
}

