package com.atlas.auth.domain.vo;

import java.time.LocalDateTime;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/23 9:30
 */
public record ApplicationVO(

        Long id,

        Long projectId,

        String applicationName,

        Boolean enabled,

        String logoUrl,

        String homePageUrl,

        String privacyPolicyUrl,

        String termsServiceUrl,

        String developerName,

        String developerEmail,

        String description,

        LocalDateTime createTime

) {
}
