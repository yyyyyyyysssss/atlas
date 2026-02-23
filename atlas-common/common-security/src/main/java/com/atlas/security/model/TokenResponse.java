package com.atlas.security.model;

import com.atlas.security.enums.TokenScheme;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
        String tokenId,
        TokenDetail access,
        TokenDetail refresh,
        TokenDetail rememberMe,
        TokenScheme tokenScheme
) {

}
