package com.atlas.auth.config.security.mfa;

/**
 * @Description
 * @Author ys
 * @Date 2026/8/26 11:59
 */
public record TotpMfaCredential (
        String code
) implements MfaCredential{
}
