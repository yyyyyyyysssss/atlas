package com.atlas.auth.controller;

import com.atlas.auth.config.security.oauth2.OAuth2ProviderAuthenticationToken;
import com.atlas.auth.domain.dto.SsoProviderAuthorizeUrlResponse;
import com.atlas.auth.domain.dto.ThirdPartyAuthRequestContext;
import com.atlas.auth.domain.dto.ThirdPartyStateContext;
import com.atlas.auth.domain.vo.ThirdPartyAuthorizeUrlVO;
import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.enums.ThirdPartyAuthAction;
import com.atlas.auth.service.ThirdPartyLoginProviderFactory;
import com.atlas.auth.service.ThirdPartyStateService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.security.model.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/8 15:01
 */
@RestController
@RequestMapping("/thirdParty")
@RequiredArgsConstructor
public class ThirdPartyLoginController {

    private final ThirdPartyLoginProviderFactory providerFactory;

    private final ThirdPartyStateService thirdPartyStateService;

    @GetMapping("/authorizeUrl/{clientName}")
    public Result<ThirdPartyAuthorizeUrlVO> authorizeUrl(@PathVariable("clientName") String clientName,
                                                         @RequestParam(value = "protocol", required = false) String protocol,
                                                         @RequestParam(value = "targetUrl", required = false) String targetUrl) {
        SsoProviderProtocol ssoProviderProtocol = StringUtils.hasText(protocol) ? SsoProviderProtocol.fromString(protocol) : SsoProviderProtocol.OAUTH2;
        ThirdPartyAuthorizeUrlVO authorizeVO = providerFactory.getProvider(clientName, ssoProviderProtocol).getAuthorizeVO(ThirdPartyAuthRequestContext.login(targetUrl));
        return ResultGenerator.ok(authorizeVO);
    }

    @GetMapping("/qrScanUrl/{clientName}")
    public Result<ThirdPartyAuthorizeUrlVO> qrScanUrl(@PathVariable("clientName") String clientName,
                                                      @RequestParam(value = "targetUrl", required = false) String targetUrl) {
        SsoProviderAuthorizeUrlResponse response = providerFactory.getProvider(clientName,SsoProviderProtocol.OAUTH2).getQrScanUrl(ThirdPartyAuthRequestContext.login(targetUrl));
        return ResultGenerator.ok(new ThirdPartyAuthorizeUrlVO(response.url(), response.state(), response.pkceRequired()));
    }

    @GetMapping("/callback/{clientName}")
    public Result<ThirdPartyCallbackVO> callback(@PathVariable("clientName") String clientName,
                                                 @RequestParam("code") String code,
                                                 @RequestParam("state") String state,
                                                 @RequestParam(value = "code_verifier", required = false) String codeVerifier) {
        OAuth2ProviderAuthenticationToken token = new OAuth2ProviderAuthenticationToken(code, state, codeVerifier);
        ThirdPartyStateContext thirdPartyStateContext = thirdPartyStateService.peekContext(state);
        ThirdPartyCallbackVO thirdPartyCallbackVO = providerFactory.getProvider(clientName,thirdPartyStateContext.getProtocol()).authenticate(token);
        return ResultGenerator.ok(thirdPartyCallbackVO);
    }

}
