package com.atlas.auth.controller;

import com.atlas.auth.config.security.oauth2.OAuth2ProviderAuthenticationToken;
import com.atlas.auth.domain.dto.OAuth2ProviderAuthorizeUrlResponse;
import com.atlas.auth.domain.vo.ThirdPartyAuthorizeUrlVO;
import com.atlas.auth.service.ThirdPartyLoginProviderFactory;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.security.model.TokenResponse;
import lombok.RequiredArgsConstructor;
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


    @GetMapping("/authorizeUrl/{clientName}")
    public Result<ThirdPartyAuthorizeUrlVO> authorizeUrl(@PathVariable("clientName") String clientName) {
        ThirdPartyAuthorizeUrlVO authorizeVO = providerFactory.getProvider(clientName).getAuthorizeVO();
        return ResultGenerator.ok(authorizeVO);
    }

    @GetMapping("/qrScanUrl/{clientName}")
    public Result<ThirdPartyAuthorizeUrlVO> qrScanUrl(@PathVariable("clientName") String clientName) {
        OAuth2ProviderAuthorizeUrlResponse response = providerFactory.getProvider(clientName).getQrScanUrl();
        return ResultGenerator.ok(new ThirdPartyAuthorizeUrlVO(response.url(), response.pkceRequired()));
    }

    @GetMapping("/callback/{clientName}")
    public Result<TokenResponse> callback(@PathVariable("clientName") String clientName,
                              @RequestParam("code") String code,
                              @RequestParam(value = "state", required = false) String state,
                              @RequestParam(value = "code_verifier", required = false) String codeVerifier) {
        OAuth2ProviderAuthenticationToken token = new OAuth2ProviderAuthenticationToken(code, state, codeVerifier);
        TokenResponse tokenResponse = providerFactory.getProvider(clientName).authenticate(token);
        return ResultGenerator.ok(tokenResponse);
    }

}
