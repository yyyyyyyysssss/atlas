package com.atlas.auth.service;

import com.atlas.auth.domain.dto.SsoProviderAuthorizeUrlResponse;
import com.atlas.auth.domain.dto.ThirdPartyAuthRequestContext;
import com.atlas.auth.domain.vo.ThirdPartyAuthorizeUrlVO;
import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
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

    default SsoProviderAuthorizeUrlResponse getAuthorizeUrl(ThirdPartyAuthRequestContext requestContext) {

        return getAuthorizeUrl(requestContext, null);
    }

    SsoProviderAuthorizeUrlResponse getAuthorizeUrl(ThirdPartyAuthRequestContext requestContext, Map<String, String> extraParams);

    default SsoProviderAuthorizeUrlResponse getQrScanUrl(ThirdPartyAuthRequestContext requestContext) {
        throw new UnsupportedOperationException("暂不支持");
    }

    ThirdPartyCallbackVO authenticate(Authentication authentication);

    default ThirdPartyAuthorizeUrlVO getAuthorizeVO(ThirdPartyAuthRequestContext requestContext) {
        SsoProviderAuthorizeUrlResponse response = getAuthorizeUrl(requestContext, null);
        return new ThirdPartyAuthorizeUrlVO(response.url(), response.state(), response.pkceRequired());
    }

}
