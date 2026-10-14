package com.atlas.auth.config.security.saml2.sp;

import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.enums.ThirdPartyCallbackStatus;
import com.atlas.auth.service.ThirdPartyLoginProviderFactory;
import com.atlas.common.security.service.TicketService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/12 14:25
 */
@Slf4j
public class Saml2SuccessHandler implements AuthenticationSuccessHandler {


    private final String uiUrl;

    private final ThirdPartyLoginProviderFactory providerFactory;

    private final TicketService ticketService;

    public Saml2SuccessHandler(String uiUrl, ThirdPartyLoginProviderFactory providerFactory, TicketService ticketService) {
        this.uiUrl = uiUrl;
        this.providerFactory = providerFactory;
        this.ticketService = ticketService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
        String registrationId = principal.getRelyingPartyRegistrationId();
        ThirdPartyCallbackVO thirdPartyCallbackVO = providerFactory.getProvider(registrationId, SsoProviderProtocol.SAML2).authenticate(authentication);
        if(!thirdPartyCallbackVO.callbackStatus().equals(ThirdPartyCallbackStatus.LOGIN)){
            String errorUrl = String.format("%s/saml2/callback?error=%s",
                    uiUrl, URLEncoder.encode("不支持的操作类型", StandardCharsets.UTF_8));
            response.sendRedirect(errorUrl);
            return;
        }
        String ticket = ticketService.create(thirdPartyCallbackVO, Duration.ofMinutes(5));
        String targetUrl = String.format(
                "%s/saml2/callback/%s?ticket=%s",
                uiUrl,
                registrationId,
                ticket
        );
        response.sendRedirect(targetUrl);
    }
}
