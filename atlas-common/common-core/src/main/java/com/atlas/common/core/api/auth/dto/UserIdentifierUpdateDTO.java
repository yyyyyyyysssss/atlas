package com.atlas.common.core.api.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserIdentifierUpdateDTO {

    @NotNull(message = "用户id不能为空")
    private Long userId;

    private String email;

    private String phone;

}
