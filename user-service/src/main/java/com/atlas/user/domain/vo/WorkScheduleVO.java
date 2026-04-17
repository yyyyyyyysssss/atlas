package com.atlas.user.domain.vo;

import java.time.LocalDateTime;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonFormat;

@Getter
@Setter
public class WorkScheduleVO {


    private Long id;

    // 用户id 
    private Long userId;

    // 事项标题 
    private String title;

    // 详细描述/备注 
    private String content;

    // 开始时间 
    private String startTime;

    // 1:紧急, 2:重要, 3:普通 
    private Integer priority;

}

