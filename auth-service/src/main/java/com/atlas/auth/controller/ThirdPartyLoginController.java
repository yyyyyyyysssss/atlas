package com.atlas.auth.controller;

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
    public Result<String> qrScanUrl(@PathVariable("clientName") String clientName) {
        String authorizeUrl = providerFactory.getProvider(clientName).getQrScanUrl();
        return ResultGenerator.ok(authorizeUrl);
    }

    @GetMapping("/callback/{clientName}")
    public Result<TokenResponse> callback(@PathVariable("clientName") String clientName,
                              @RequestParam("code") String code,
                              @RequestParam(value = "state", required = false) String state,
                              @RequestParam(value = "code_verifier", required = false) String codeVerifier) {
        TokenResponse tokenResponse = providerFactory.getProvider(clientName).processCallback(code, state,codeVerifier);
        return ResultGenerator.ok(tokenResponse);
    }

}
