package com.atlas.auth.domain.vo;

public record VerifyCaptchaVO(
        boolean verified,

        String ticket
) {
}
