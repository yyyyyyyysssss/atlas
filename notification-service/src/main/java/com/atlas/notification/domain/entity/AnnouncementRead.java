package com.atlas.notification.domain.entity;

import java.time.LocalDateTime;
import java.time.LocalDateTime;

import com.atlas.common.mybatis.entity.BaseIdEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Tolerate;

/**
 * (AnnouncementRead)实体类
 *
 * @author ys
 * @since 2026-03-30 13:46:43
 */
@Getter
@Setter
@TableName(value = "announcement_read", autoResultMap = true)
@Builder
public class AnnouncementRead extends BaseIdEntity {

    @Tolerate
    public AnnouncementRead() {
    }

    // 公告id 
    @TableField("announcement_id")
    private Long announcementId;

    // 用户id 
    @TableField("user_id")
    private Long userId;

    // 阅读时间 
    @TableField("read_time")
    private LocalDateTime readTime;


}

