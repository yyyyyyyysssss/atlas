package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OAuth2ClientSaveDTO(

        Long id,

        @NotBlank(message = "应用名称不能为空")
        String clientName,

        @NotEmpty(message = "请至少配置一个回调地址")
        List<String> redirectUri,

        @NotEmpty(message = "申请授权范围不能为空")
        List<String> scopes,

        Boolean allowDeviceFlow,

        String jwksUri,

        String description
) {
}
