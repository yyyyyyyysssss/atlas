package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/23 9:52
 */
public record ApplicationSaveDTO(

        Long id,

        @NotBlank(message = "应用名称不能为空")
        String applicationName,

        Boolean enabled,

        String logoUrl,

        @NotBlank(message = "应用主页不能为空")
        String homePageUrl,

        String privacyPolicyUrl,

        String termsServiceUrl,

        String developerName,

        String developerEmail,

        String description

) {


}
