package com.atlas.auth.service;

import com.atlas.auth.domain.dto.OAuth2ProviderAuthorizeUrlResponse;
import com.atlas.auth.domain.dto.Saml2ProviderSettings;
import com.atlas.auth.domain.dto.Saml2UserInfo;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.properties.SecurityProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/16 11:07
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Auth0LoginProvider extends AbstractThirdPartyLoginProvider{

    private final SecurityProperties securityProperties;

    @Override
    public String getProviderName() {
        return "auth0";
    }

    @Override
    public OAuth2ProviderAuthorizeUrlResponse getAuthorizeUrl() {
        String saml2AuthUrl = securityProperties.getSaml2AuthUrl();
        String finalSaml2AuthUrl = saml2AuthUrl.replace("{registrationId}", getProviderName());
        return new OAuth2ProviderAuthorizeUrlResponse(finalSaml2AuthUrl, false);
    }

    @Override
    public TokenResponse authenticate(Authentication authentication) {
        String providerName = getProviderName();
        Saml2ProviderSettings saml2ProviderSettings = ssoProviderService.getSettings(providerName, SsoProviderProtocol.SAML2);
        // 配置的映射对象
        Saml2ProviderSettings.Mapping mappings = saml2ProviderSettings.assertingparty().mappings();

        Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
        Saml2UserInfo saml2User = Saml2UserInfo.fromPrincipal(principal, mappings);
        saml2User.setProvider(providerName);

        return doLogin(saml2User);
    }

}
