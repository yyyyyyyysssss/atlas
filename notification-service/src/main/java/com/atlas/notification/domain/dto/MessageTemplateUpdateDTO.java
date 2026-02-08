package com.atlas.notification.domain.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageTemplateUpdateDTO extends MessageTemplateCreateDTO {

    @NotNull(message = "id不能为空")
    private Long id;


}

