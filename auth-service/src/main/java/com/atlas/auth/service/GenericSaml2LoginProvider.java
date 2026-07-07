package com.atlas.auth.service;

import com.atlas.auth.domain.dto.SsoProviderAuthorizeUrlResponse;
import com.atlas.auth.domain.dto.Saml2ProviderSettings;
import com.atlas.auth.domain.dto.Saml2UserInfo;
import com.atlas.auth.domain.dto.ThirdPartyAuthRequestContext;
import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.enums.ThirdPartyAuthAction;
import com.atlas.security.model.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
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
    public ThirdPartyCallbackVO authenticate(Authentication authentication) {
        String providerName = getProviderName();
        Saml2ProviderSettings saml2ProviderSettings = ssoProviderService.getSettings(providerName, SsoProviderProtocol.SAML2);
        // 配置的映射对象
        Saml2ProviderSettings.Mapping mappings = saml2ProviderSettings.assertingparty().mappings();
        Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
        Saml2UserInfo saml2User = Saml2UserInfo.fromPrincipal(principal, mappings);
        saml2User.setProvider(providerName);

        return dispatchFederatedIdentity(saml2User);
    }
}
