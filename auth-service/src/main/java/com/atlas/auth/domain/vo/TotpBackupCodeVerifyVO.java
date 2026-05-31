package com.atlas.auth.domain.vo;

public record TotpBackupCodeVerifyVO(
        boolean verified,

        String ticket
) {
}
