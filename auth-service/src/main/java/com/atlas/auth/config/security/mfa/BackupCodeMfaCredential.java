package com.atlas.auth.config.security.mfa;

public record BackupCodeMfaCredential (

        String code

) implements MfaCredential {
}
