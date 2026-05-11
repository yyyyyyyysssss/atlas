package com.atlas.auth.service;

import com.atlas.security.model.TokenResponse;

public interface ThirdPartyLoginProvider {

    String getProviderName();

    String getAuthorizeUrl();

    default String getQrScanUrl(){
        throw new UnsupportedOperationException("暂不支持");
    }

    TokenResponse processCallback(String code,String state);

}
