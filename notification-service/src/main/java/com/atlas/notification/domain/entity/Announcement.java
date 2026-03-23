package com.atlas.notification.domain.entity;

import com.atlas.common.mybatis.entity.BaseEntity;
import com.atlas.notification.enums.AnnouncementStatus;
import com.atlas.notification.enums.AnnouncementType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;

/**
 * (Announcement)实体类
 *
 * @author ys
 * @since 2026-03-23 14:57:28
 */
@Getter
@Setter
@TableName(value = "announcement", autoResultMap = true)
@Builder
public class Announcement extends BaseEntity {

    @Tolerate
    public Announcement() {
    }

    // 公告标题 
    @TableField("title")
    private String title;

    // 列表摘要 
    @TableField("description")
    private String description;

    // 存储标准 MD 字符串 
    @TableField("content")
    private String content;

    // 公告类型：URGENT:紧急, RELEASE:发版, NOTICE:通知, MAINTAIN:维护 
    @TableField("type")
    private AnnouncementType type;

    // 版本号 
    @TableField("version")
    private String version;

    // DRAFT:草稿, PUBLISHED:已发布, RECALLED:已撤回 
    @TableField("status")
    private AnnouncementStatus status;

    // 优先级：数字越大越靠前 
    @TableField("priority")
    private Integer priority;

    // 发布时间 
    @TableField("publish_time")
    private LocalDateTime publishTime;


}

