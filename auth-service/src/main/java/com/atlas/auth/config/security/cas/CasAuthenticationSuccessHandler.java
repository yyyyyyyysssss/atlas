package com.atlas.auth.config.security.cas;

import com.atlas.auth.enums.ClientProtocol;
import com.atlas.auth.event.ClientAuthorizedEvent;
import com.atlas.common.security.model.SecurityUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Objects;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/12 11:59
 */
public class CasAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private ApplicationEventPublisher eventPublisher;

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public CasAuthenticationSuccessHandler(ApplicationEventPublisher eventPublisher){
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CasAuthenticationToken casAuthenticationToken = (CasAuthenticationToken)authentication;

        CasRequestContext casRequestContext = casAuthenticationToken.getCasRequestContext();
        String ticket = casAuthenticationToken.getTicket();
        String service = casRequestContext.service();
        String relayState = casRequestContext.relayState();

        // 发布客户端授权事件
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        if(eventPublisher != null){
            eventPublisher.publishEvent(new ClientAuthorizedEvent(this, securityUser.getId(), service, ClientProtocol.CAS));
        }

        // 构建客户端回调地址
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(service)
                .queryParam("ticket", ticket);

        if (relayState != null && !relayState.isBlank()) {
            builder.queryParam("relayState", relayState);
        }

        String redirectUrl = builder
                .build()
                .encode()
                .toUriString();

        redirectStrategy.sendRedirect(request, response, redirectUrl);
    }

    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        Objects.requireNonNull(eventPublisher);
        this.eventPublisher = eventPublisher;
    }
}
