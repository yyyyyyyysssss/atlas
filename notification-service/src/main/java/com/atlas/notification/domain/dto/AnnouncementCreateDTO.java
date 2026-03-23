package com.atlas.notification.domain.dto;

import java.time.LocalDateTime;
import java.time.LocalDateTime;

import com.atlas.notification.enums.AnnouncementStatus;
import com.atlas.notification.enums.AnnouncementType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnouncementCreateDTO {

    // 公告标题 
    private String title;

    // 列表摘要 
    private String description;

    // 存储标准 MD 字符串 
    private String content;

    // 公告类型：URGENT:紧急, RELEASE:发版, NOTICE:通知, MAINTAIN:维护 
    private AnnouncementType type;

    // 版本号 
    private String version;

    // DRAFT:草稿, PUBLISHED:已发布, RECALLED:已撤回 
    private AnnouncementStatus status;

    // 优先级：数字越大越靠前 
    private Integer priority;

    // 发布时间 
    private LocalDateTime publishTime;


}

