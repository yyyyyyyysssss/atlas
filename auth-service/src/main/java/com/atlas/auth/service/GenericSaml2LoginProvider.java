package com.atlas.auth.service;

import com.atlas.auth.config.exception.AuthErrorCode;
import com.atlas.auth.config.exception.ThirdPartyAuthException;
import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class GenericSaml2LoginProvider extends AbstractThirdPartyLoginProvider{

    private final String providerName;

    private final String saml2AuthUrl;

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public SsoProviderProtocol protocol() {
        return SsoProviderProtocol.SAML2;
    }

    @Override
    public SsoProviderAuthorizeUrlResponse getAuthorizeUrl(ThirdPartyAuthRequestContext requestContext, Map<String, String> extraParams) {
        // 动态替换注册 ID
        String finalSaml2AuthUrl = saml2AuthUrl.replace("{registrationId}", getProviderName());
        return SsoProviderAuthorizeUrlResponse.of(finalSaml2AuthUrl, false);
    }

    @Override
    public ThirdPartyCallbackVO authenticate(ThirdPartyLoginRequest request) {
        Saml2LoginRequest saml2 = (Saml2LoginRequest)request;
        String providerName = getProviderName();
        Saml2ProviderSettings saml2ProviderSettings = ssoProviderService.getSettings(providerName, SsoProviderProtocol.SAML2);
        if (saml2ProviderSettings == null) {
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_PROVIDER_NOT_FOUND, providerName, SsoProviderProtocol.SAML2);
        }
        // 配置的映射对象
        Saml2ProviderSettings.Mapping mappings = saml2ProviderSettings.mappings();
        Saml2AuthenticatedPrincipal principal = saml2.principal();
        Saml2UserInfo saml2User = Saml2UserInfo.fromPrincipal(principal, mappings);
        saml2User.setProvider(providerName);

        return dispatchFederatedIdentity(saml2User);
    }
}
