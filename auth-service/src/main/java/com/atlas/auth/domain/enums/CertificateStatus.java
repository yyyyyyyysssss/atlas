package com.atlas.auth.domain.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CertificateStatus {

    VALID("VALID", "有效"),
    EXPIRING("EXPIRING", "即将过期"),
    EXPIRED("EXPIRED", "已过期"),
    DISABLED("DISABLED", "已停用"),
    REVOKED("REVOKED", "已吊销");

    private final String value;

    private final String label;

}
