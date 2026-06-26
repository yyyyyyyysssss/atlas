package com.atlas.auth.config.security.saml2;

import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.enums.ThirdPartyCallbackStatus;
import com.atlas.auth.service.ThirdPartyLoginProviderFactory;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.properties.SecurityProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
        ThirdPartyCallbackVO thirdPartyCallbackVO = providerFactory.getProvider(registrationId, SsoProviderProtocol.SAML2).authenticate(authentication);
        if(!thirdPartyCallbackVO.callbackStatus().equals(ThirdPartyCallbackStatus.LOGIN)){
            String uiUrl = securityProperties.getUiUrl();
            String errorUrl = String.format("%s/saml2/callback?error=%s",
                    uiUrl, URLEncoder.encode("不支持的操作类型", StandardCharsets.UTF_8));
            response.sendRedirect(errorUrl);
            return;
        }
        TokenResponse tokenResponse = thirdPartyCallbackVO.tokenResponse();
        String uiUrl = securityProperties.getUiUrl();
        String targetUrl = String.format("%s/saml2/callback/%s?accessToken=%s&refreshToken=%s",
                uiUrl, registrationId, tokenResponse.token().access().value(), tokenResponse.token().refresh().value());
        response.sendRedirect(targetUrl);
    }
}
