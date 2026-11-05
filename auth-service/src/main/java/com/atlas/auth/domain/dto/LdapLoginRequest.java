package com.atlas.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LdapLoginRequest(

        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password
) implements ThirdPartyLoginRequest {

}
