package com.atlas.user.domain.dto;


import java.time.LocalDateTime;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class OrganizationUpdateDTO extends OrganizationCreateDTO {

    @NotNull(message = "id不能为空")
    private Long id;


}

