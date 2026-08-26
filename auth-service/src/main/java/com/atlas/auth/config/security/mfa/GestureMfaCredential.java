package com.atlas.auth.config.security.mfa;

public record GestureMfaCredential(

        String code

) implements MfaCredential {
}
