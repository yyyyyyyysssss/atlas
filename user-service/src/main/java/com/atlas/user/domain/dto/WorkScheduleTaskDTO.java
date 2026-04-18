package com.atlas.user.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class WorkScheduleTaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private LocalDateTime startTime;

}
