package com.atlas.auth.domain.vo;

import com.atlas.auth.domain.enums.CertificateStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/25 14:29
 */

@Data
@Builder
public class UserCertificateVO {

    private Long id;

    private Long userId;

    private String label;

    private CertificateStatus status;

    private LocalDateTime notAfter;

    private Integer expireDays;

    private LocalDateTime createTime;

}
