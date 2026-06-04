package com.atlas.auth.domain.vo;

public record GestureVerifyVO(
        boolean verified,

        String ticket
) {
}
