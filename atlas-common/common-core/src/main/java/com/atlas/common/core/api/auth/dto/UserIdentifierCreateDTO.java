package com.atlas.common.core.api.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserIdentifierCreateDTO {

    @NotNull(message = "用户id不能为空")
    private Long userId;

    @NotNull(message = "用户账号不能为空")
    private String username;

    private String email;

    private String phone;

}
