package com.atlas.auth.controller;

import com.atlas.auth.domain.vo.QrAuthStatusVO;
import com.atlas.auth.domain.vo.QrAuthTicketVO;
import com.atlas.auth.service.QrAuthService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.security.resolver.NormalBearerTokenResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/oauth2/qr")
public class QrAuthController {

    private final QrAuthService qrAuthService;

    private final NormalBearerTokenResolver normalBearerTokenResolver;

    @GetMapping("/ticket")
    public Result<QrAuthTicketVO> ticket(@RequestParam("client_id") String clientId,
                                         @RequestParam("redirect_uri") String redirectUri,
                                         @RequestParam("scope") String scope,
                                         @RequestParam(value = "code_challenge", required = false) String codeChallenge,
                                         @RequestParam(value = "code_challenge_method",required = false) String codeChallengeMethod) {
        QrAuthTicketVO ticket = qrAuthService.ticket(clientId, redirectUri, scope,codeChallenge,codeChallengeMethod);
        return ResultGenerator.ok(ticket);
    }

    @PostMapping("/scan")
    public Result<Void> scan(@RequestParam("sceneId") String sceneId) {
        qrAuthService.scan(sceneId);
        return ResultGenerator.ok();
    }

    @PostMapping("/confirm")
    public Result<QrAuthStatusVO> confirm(@RequestParam("sceneId") String sceneId, HttpServletRequest request) {
        String token = normalBearerTokenResolver.resolve(request);
        qrAuthService.confirm(sceneId, token);
        return ResultGenerator.ok();
    }

    @GetMapping("/status")
    public Result<QrAuthStatusVO> status(@RequestParam("sceneId") String sceneId) {
        QrAuthStatusVO status = qrAuthService.status(sceneId);
        return ResultGenerator.ok(status);
    }

}
