package com.atlas.auth.domain.vo;

public record VerifyPasswordVO(
        boolean verified,

        String ticket
) {
}
