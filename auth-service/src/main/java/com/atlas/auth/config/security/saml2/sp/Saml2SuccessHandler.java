package com.atlas.auth.config.security.saml2.sp;

import com.atlas.auth.domain.dto.Saml2LoginRequest;
import com.atlas.auth.domain.dto.ThirdPartyLoginRequest;
import com.atlas.common.security.service.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/12 14:25
 */
@Slf4j
public class Saml2SuccessHandler implements AuthenticationSuccessHandler {


    private final String uiUrl;

    private final TicketService ticketService;

    public Saml2SuccessHandler(String uiUrl, TicketService ticketService) {
        this.uiUrl = uiUrl;
        this.ticketService = ticketService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
        String registrationId = principal.getRelyingPartyRegistrationId();
        ThirdPartyLoginRequest thirdPartyLoginRequest = Saml2LoginRequest.from(principal);
        String ticket = ticketService.create(thirdPartyLoginRequest, Duration.ofMinutes(5));
        String targetUrl = String.format(
                "%s/saml2/callback/%s?ticket=%s",
                uiUrl,
                registrationId,
                ticket
        );
        response.sendRedirect(targetUrl);
    }
}
