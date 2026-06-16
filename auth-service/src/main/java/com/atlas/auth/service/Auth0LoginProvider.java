package com.atlas.auth.service;

import com.atlas.auth.domain.vo.ThirdPartyAuthorizeUrlVO;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    public String getAuthorizeUrl() {
        String saml2AuthUrl = securityProperties.getSaml2AuthUrl();
        return saml2AuthUrl.replace("{registrationId}",getProviderName());
    }

    @Override
    public boolean isPKCERequired() {
        return false;
    }

    @Override
    public TokenResponse processCallback(String code, String state, String codeVerifier) {
        log.warn("Provider {} uses SAML2, processCallback should not be called.", getProviderName());
        throw new UnsupportedOperationException("SAML2 callback is handled by Spring Security filter chain");
    }
}
