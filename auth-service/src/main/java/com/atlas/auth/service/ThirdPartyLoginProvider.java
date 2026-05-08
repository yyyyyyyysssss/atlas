package com.atlas.auth.service;

import com.atlas.security.model.TokenResponse;

public interface ThirdPartyLoginProvider {

    String getProviderName();

    String getAuthorizeUrl();

    TokenResponse processCallback(String code,String state);

}
