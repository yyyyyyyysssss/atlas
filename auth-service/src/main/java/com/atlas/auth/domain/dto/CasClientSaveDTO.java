package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public record CasClientSaveDTO(

        Long id,

        @NotBlank(message = "客户端名称不能为空")
        String clientName,

        @NotBlank(message = "service不能为空")
        String service,

        Boolean proxyEnabled,

        List<String> proxyCallbackUrls,

        Map<String, String> attributeMappings,

        String description
) {
}
