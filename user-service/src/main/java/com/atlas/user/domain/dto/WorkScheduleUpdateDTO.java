package com.atlas.user.domain.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkScheduleUpdateDTO extends WorkScheduleCreateDTO {

    @NotNull(message = "id不能为空")
    private Long id;


}

