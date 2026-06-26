package com.atlas.auth.domain.vo;

import com.atlas.auth.enums.ThirdPartyCallbackStatus;
import com.atlas.security.model.TokenResponse;

public record ThirdPartyCallbackVO(

        ThirdPartyCallbackStatus callbackStatus,

        TokenResponse tokenResponse,

        String targetUrl

) {

    public static ThirdPartyCallbackVO loginSuccess(TokenResponse tokenResponse, String targetUrl) {
        return new ThirdPartyCallbackVO(ThirdPartyCallbackStatus.LOGIN, tokenResponse, targetUrl);
    }

    public static ThirdPartyCallbackVO bindSuccess(String targetUrl) {
        return new ThirdPartyCallbackVO(ThirdPartyCallbackStatus.BIND, null, targetUrl);
    }

}
