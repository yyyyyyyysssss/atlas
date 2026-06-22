package com.atlas.auth.service;

import com.atlas.auth.domain.dto.OAuth2ProviderAuthorizeUrlResponse;
import com.atlas.auth.domain.vo.ThirdPartyAuthorizeUrlVO;
import com.atlas.auth.enums.ThirdPartyAuthAction;
import com.atlas.security.model.TokenResponse;
import org.springframework.security.core.Authentication;

public interface ThirdPartyLoginProvider {

    String getProviderName();

    OAuth2ProviderAuthorizeUrlResponse getAuthorizeUrl(ThirdPartyAuthAction action);

    default OAuth2ProviderAuthorizeUrlResponse getQrScanUrl(){
        throw new UnsupportedOperationException("暂不支持");
    }

    TokenResponse authenticate(Authentication authentication);

    default ThirdPartyAuthorizeUrlVO getAuthorizeVO(ThirdPartyAuthAction action) {
        OAuth2ProviderAuthorizeUrlResponse response = getAuthorizeUrl(action);
        return new ThirdPartyAuthorizeUrlVO(response.url(), response.pkceRequired());
    }

}
