package com.atlas.auth.domain.vo;

public record ThirdPartyAuthorizeUrlVO(

        String authorizeUrl,

        Boolean isPKCERequired
) {
}
