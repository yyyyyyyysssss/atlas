package com.atlas.auth.controller;

import com.atlas.auth.domain.vo.QrAuthStatusVO;
import com.atlas.auth.domain.vo.QrAuthTicketVO;
import com.atlas.auth.service.QrAuthService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.security.model.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/oauth2/qr")
public class QrAuthController {

    private final QrAuthService qrAuthService;

    @GetMapping("/ticket")
    public Result<QrAuthTicketVO> ticket(@RequestParam("client_id") String clientId,
                                         @RequestParam("redirect_uri") String redirectUri,
                                         @RequestParam("scope") String scope,
                                         @RequestParam("state") String state,
                                         @RequestParam(value = "code_challenge", required = false) String codeChallenge,
                                         @RequestParam(value = "code_challenge_method",required = false) String codeChallengeMethod) {
        QrAuthTicketVO ticket = qrAuthService.ticket(clientId, redirectUri, scope,state, codeChallenge,codeChallengeMethod);
        return ResultGenerator.ok(ticket);
    }

    @PostMapping("/scan")
    public Result<Void> scan(@RequestParam("sceneId") String sceneId) {
        qrAuthService.scan(sceneId);
        return ResultGenerator.ok();
    }

    @PostMapping("/confirm")
    public Result<QrAuthStatusVO> confirm(@AuthenticationPrincipal SecurityUser securityUser, @RequestParam("sceneId") String sceneId) {
        qrAuthService.confirm(sceneId, securityUser.getTokenId());
        return ResultGenerator.ok();
    }

    @GetMapping("/status")
    public Result<QrAuthStatusVO> status(@RequestParam("sceneId") String sceneId) {
        QrAuthStatusVO status = qrAuthService.status(sceneId);
        return ResultGenerator.ok(status);
    }

}
