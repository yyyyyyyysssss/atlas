package com.atlas.auth.domain.vo;

public record TotpInitVO(
        String otpAuthUrl,

        String secret
) {
}
