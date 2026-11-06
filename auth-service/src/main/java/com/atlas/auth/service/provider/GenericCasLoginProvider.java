package com.atlas.auth.service.provider;

import com.atlas.auth.config.exception.AuthErrorCode;
import com.atlas.auth.config.exception.ThirdPartyAuthException;
import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.common.security.properties.SecurityProperties;
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

    private final SecurityProperties.CasConfig casConfig;

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
        if (settings == null) {
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
        CasLoginRequest casLoginRequest = (CasLoginRequest) request;
        CasProviderSettings settings = ssoProviderService.getSettings(providerName, SsoProviderProtocol.CAS);
        if (settings == null) {
            throw new ThirdPartyAuthException(AuthErrorCode.THIRD_PARTY_PROVIDER_NOT_FOUND, providerName, SsoProviderProtocol.CAS);
        }

        CasProviderEngine.CasAuthenticationSuccess principal = casProviderEngine.validateServerTicket(
                providerName,
                settings,
                casLoginRequest.ticket(),
                null
        );

        CasUserInfo casUserInfo = CasUserInfo.fromPrincipal(principal, settings.mappings());
        casUserInfo.setProvider(providerName);

        return dispatchFederatedIdentity(casUserInfo);
    }

    /**
     * 测试 CAS 代理票据完整流程。
     * ST -> PGT -> PT -> Proxy Validation
     */
    private CasProviderEngine.CasAuthenticationSuccess testProxyTicket(CasProviderSettings settings, String ticket) {
        // 验证 ST，申请 PGT。
        CasProviderEngine.CasAuthenticationSuccess principal = casProviderEngine.validateServerTicket(
                providerName,
                settings,
                ticket,
                casConfig.getPgtUrl()
        );
        // 获取 PGTIOU。
        String pgtIou = principal.getProxyGrantingTicket();

        // 根据 PGTIOU 获取 PGT。
        String pgt = casProviderEngine.getProxyGrantingTicket(pgtIou);

        // 使用 PGT 申请 PT。
        String proxyTicket = casProviderEngine.getProxyTicket(providerName, settings, pgt, settings.serviceUrl());

        // 使用 PT 验证代理票据。
        return casProviderEngine.validateProxyTicket(providerName, settings, proxyTicket);
    }


}
