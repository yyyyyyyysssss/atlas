package com.atlas.auth.domain.vo;

public record VerifyWebauthnVO(
        boolean verified,

        String ticket
) {
}
