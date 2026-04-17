package com.atlas.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WorkScheduleCreateDTO {

    // 用户id
    private Long userId;

    // 事项标题
    @NotBlank(message = "事项标题不能为空")
    private String title;

    // 详细描述/备注 
    private String content;

    // 开始时间
    @NotNull(message = "时间不能为空")
    private LocalDateTime startTime;

    // 1:紧急, 2:重要, 3:普通 
    private Integer priority = 3;


}

