package com.atlas.auth.domain.vo;

public record ThirdPartyProviderBindVO(

        String authorizeUrl,

        String state,

        boolean isPKCERequired

) {
}
