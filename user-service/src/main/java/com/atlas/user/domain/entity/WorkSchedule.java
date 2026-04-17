package com.atlas.user.domain.entity;

import com.atlas.common.mybatis.entity.BaseIdEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;

/**
 * (WorkSchedule)实体类
 *
 * @author ys
 * @since 2026-04-17 15:44:54
 */
@Getter
@Setter
@TableName(value = "work_schedule", autoResultMap = true)
@Builder
public class WorkSchedule extends BaseIdEntity {

    @Tolerate
    public WorkSchedule() {
    }

    // 用户id 
    @TableField("user_id")
    private Long userId;

    // 事项标题 
    @TableField("title")
    private String title;

    // 详细描述/备注 
    @TableField("content")
    private String content;

    // 开始时间 
    @TableField("start_time")
    private LocalDateTime startTime;

    // 1:紧急, 2:重要, 3:普通 
    @TableField("priority")
    private Integer priority;


}

