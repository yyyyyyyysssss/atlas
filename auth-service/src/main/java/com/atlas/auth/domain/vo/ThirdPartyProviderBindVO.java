package com.atlas.auth.domain.vo;

public record ThirdPartyProviderBindVO(

        String authorizeUrl,

        boolean isPKCERequired

) {
}
