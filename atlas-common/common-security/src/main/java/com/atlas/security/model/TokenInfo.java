package com.atlas.security.model;

import com.atlas.security.enums.TokenScheme;

public record TokenInfo(
        String id,
        Token access,
        Token refresh,
        TokenScheme tokenScheme
) {

    public record Token(
            String value,
            Long expiresIn
    ) {}

}
