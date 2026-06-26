package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.ThirdPartyAuthAction;

public record ThirdPartyAuthRequestContext(

        ThirdPartyAuthAction action,

        String state,

        String targetUrl
) {

    public static ThirdPartyAuthRequestContext login() {
        return new ThirdPartyAuthRequestContext(ThirdPartyAuthAction.LOGIN, null, null);
    }

    public static ThirdPartyAuthRequestContext login(String targetUrl) {
        return new ThirdPartyAuthRequestContext(ThirdPartyAuthAction.LOGIN,null, targetUrl);
    }

    public static ThirdPartyAuthRequestContext bind() {
        return new ThirdPartyAuthRequestContext(ThirdPartyAuthAction.BIND,null, null);
    }

    public static ThirdPartyAuthRequestContext bind(String targetUrl) {
        return new ThirdPartyAuthRequestContext(ThirdPartyAuthAction.BIND,null, targetUrl);
    }

    public static ThirdPartyAuthRequestContext of(ThirdPartyAuthAction action, String state, String targetUrl) {
        return new ThirdPartyAuthRequestContext(action, state, targetUrl);
    }

}
