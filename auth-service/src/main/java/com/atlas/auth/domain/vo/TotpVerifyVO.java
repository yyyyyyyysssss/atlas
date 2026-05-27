package com.atlas.auth.domain.vo;

public record TotpVerifyVO(
        boolean verified,

        String ticket
) {
}
