package com.atlas.auth.domain.dto;

import com.atlas.security.enums.ClientType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PasswordLoginDTO(

        @NotBlank(message = "用户名不能为空")
        String username,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "密码不能为空")
        String password,

        @NotNull(message = "客户端类型不能为空")
        ClientType clientType
) {
}
