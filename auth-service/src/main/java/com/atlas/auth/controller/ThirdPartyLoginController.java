package com.atlas.auth.controller;

import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.vo.ThirdPartyAuthorizeUrlVO;
import com.atlas.auth.domain.vo.ThirdPartyCallbackVO;
import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.service.ThirdPartyLoginProviderFactory;
import com.atlas.auth.service.ThirdPartyStateService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.common.security.service.TicketService;
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

    private final TicketService ticketService;

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
                                                 @RequestParam(value = "code", required = false) String code,
                                                 @RequestParam(value = "state", required = false) String state,
                                                 @RequestParam(value = "error", required = false) String error,
                                                 @RequestParam(value = "code_verifier", required = false) String codeVerifier) {
        // 用户拒绝授权
        if (StringUtils.hasText(error)) {
            return ResultGenerator.ok(ThirdPartyCallbackVO.denied(error));
        }
        // 授权成功
        ThirdPartyLoginRequest request = new OAuth2LoginRequest(code, state, codeVerifier);
        ThirdPartyStateContext thirdPartyStateContext = thirdPartyStateService.peekContext(state);
        ThirdPartyCallbackVO thirdPartyCallbackVO = providerFactory.getProvider(clientName,thirdPartyStateContext.getProtocol()).authenticate(request);
        return ResultGenerator.ok(thirdPartyCallbackVO);
    }

    @GetMapping("/callback/ticket")
    public Result<ThirdPartyCallbackVO> callbackTicket(@RequestParam("ticket") String ticket) {
        ThirdPartyCallbackVO callbackVO = ticketService.consume(ticket, ThirdPartyCallbackVO.class);
        return ResultGenerator.ok(callbackVO);
    }

    @PostMapping("/ldap/login/{clientName}")
    public Result<ThirdPartyCallbackVO> ldapLogin(@PathVariable("clientName") String clientName, @RequestBody LdapLoginRequest request) {
        ThirdPartyCallbackVO callbackVO = providerFactory.getProvider(clientName, SsoProviderProtocol.LDAP).authenticate(request);
        return ResultGenerator.ok(callbackVO);
    }


}
