package com.atlas.auth.config.security.saml2;

import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.service.ThirdPartyLoginProviderFactory;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.properties.SecurityProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/12 14:25
 */
@Component
@Slf4j
public class Saml2SuccessHandler implements AuthenticationSuccessHandler {


    private final SecurityProperties securityProperties;

    private final ThirdPartyLoginProviderFactory providerFactory;

    public Saml2SuccessHandler(SecurityProperties securityProperties, ThirdPartyLoginProviderFactory providerFactory) {
        this.securityProperties = securityProperties;
        this.providerFactory = providerFactory;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
        String registrationId = principal.getRelyingPartyRegistrationId();
        TokenResponse tokenResponse = providerFactory.getProvider(registrationId, SsoProviderProtocol.SAML2).authenticate(authentication);

        String uiUrl = securityProperties.getUiUrl();
        String targetUrl = String.format("%s/saml2/callback/%s?accessToken=%s&refreshToken=%s",
                uiUrl, registrationId, tokenResponse.token().access().value(), tokenResponse.token().refresh().value());
        response.sendRedirect(targetUrl);
    }
}
