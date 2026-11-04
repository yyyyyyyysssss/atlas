package com.atlas.auth.service.provider;

import com.atlas.auth.config.exception.AuthErrorCode;
import com.atlas.auth.config.exception.ThirdPartyAuthException;
import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/11/4 14:02
 */

@Slf4j
@RequiredArgsConstructor
public class GenericCasLoginProvider extends AbstractThirdPartyLoginProvider {

    private final String providerName;

    private final CasProviderEngine casProviderEngine;

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public SsoProviderProtocol protocol() {
        return SsoProviderProtocol.CAS;
    }

    @Override
    public SsoProviderAuthorizeUrlResponse getAuthorizeUrl(ThirdPartyAuthRequestContext requestContext, Map<String, String> extraParams) {
        CasProviderSettings settings = ssoProviderService.getSettings(getProviderName(), SsoProviderProtocol.CAS);
        if(settings == null){
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_PROVIDER_NOT_FOUND, providerName, SsoProviderProtocol.CAS);
        }
        String url = UriComponentsBuilder
                .fromUriString(settings.loginUrl())
                .queryParam("service", settings.serviceUrl())
                .build()
                .encode()
                .toUriString();
        return SsoProviderAuthorizeUrlResponse.of(url, false);
    }

    @Override
    public ThirdPartyCallbackVO authenticate(ThirdPartyLoginRequest request) {
        CasLoginRequest casLoginRequest = (CasLoginRequest)request;
        CasProviderSettings settings = ssoProviderService.getSettings(providerName, SsoProviderProtocol.CAS);
        if(settings == null){
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_PROVIDER_NOT_FOUND, providerName, SsoProviderProtocol.CAS);
        }
        CasProviderEngine.CasAuthenticationSuccess principal = casProviderEngine.validateTicket(providerName, settings, casLoginRequest.ticket());
        CasUserInfo casUserInfo = CasUserInfo.fromPrincipal(principal, settings.mappings());
        casUserInfo.setProvider(providerName);

        return dispatchFederatedIdentity(casUserInfo);
    }
}
