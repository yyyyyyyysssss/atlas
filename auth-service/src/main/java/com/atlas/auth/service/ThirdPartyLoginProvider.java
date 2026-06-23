package com.atlas.auth.service;

import com.atlas.auth.domain.dto.SsoProviderAuthorizeUrlResponse;
import com.atlas.auth.domain.vo.ThirdPartyAuthorizeUrlVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.enums.ThirdPartyAuthAction;
import com.atlas.security.model.TokenResponse;
import org.springframework.security.core.Authentication;

import java.util.Map;

public interface ThirdPartyLoginProvider {

    String getProviderName();

    default SsoProviderProtocol protocol() {
        return SsoProviderProtocol.OAUTH2;
    }

    default SsoProviderAuthorizeUrlResponse getAuthorizeUrl(){

        return getAuthorizeUrl(ThirdPartyAuthAction.LOGIN, null);
    }

    default SsoProviderAuthorizeUrlResponse getAuthorizeUrl(ThirdPartyAuthAction action){

        return getAuthorizeUrl(action, null);
    }

    SsoProviderAuthorizeUrlResponse getAuthorizeUrl(ThirdPartyAuthAction action, Map<String, String> extraParams);

    default SsoProviderAuthorizeUrlResponse getQrScanUrl(){
        throw new UnsupportedOperationException("暂不支持");
    }

    TokenResponse authenticate(Authentication authentication);

    default ThirdPartyAuthorizeUrlVO getAuthorizeVO(ThirdPartyAuthAction action) {
        SsoProviderAuthorizeUrlResponse response = getAuthorizeUrl(action, null);
        return new ThirdPartyAuthorizeUrlVO(response.url(), response.pkceRequired());
    }

}
