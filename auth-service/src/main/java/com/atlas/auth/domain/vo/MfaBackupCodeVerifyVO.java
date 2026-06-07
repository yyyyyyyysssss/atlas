package com.atlas.auth.domain.vo;

public record MfaBackupCodeVerifyVO(
        boolean verified,

        String ticket
) {
}
