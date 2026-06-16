package com.atlas.auth.service;

import com.atlas.auth.domain.vo.ThirdPartyAuthorizeUrlVO;
import com.atlas.security.model.TokenResponse;

public interface ThirdPartyLoginProvider {

    String getProviderName();

    boolean isPKCERequired();

    String getAuthorizeUrl();

    default String getQrScanUrl(){
        throw new UnsupportedOperationException("暂不支持");
    }

    TokenResponse processCallback(String code,String state,String codeVerifier);

    default ThirdPartyAuthorizeUrlVO getAuthorizeVO() {
        return new ThirdPartyAuthorizeUrlVO(getAuthorizeUrl(), isPKCERequired());
    }

}
