package com.atlas.notification.domain.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnouncementUpdateDTO extends AnnouncementCreateDTO {

    @NotNull(message = "id不能为空")
    private Long id;


}

