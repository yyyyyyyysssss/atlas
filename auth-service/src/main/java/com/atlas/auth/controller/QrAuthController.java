package com.atlas.auth.controller;

import com.atlas.auth.domain.vo.QrAuthStatusVO;
import com.atlas.auth.domain.vo.QrAuthTicketVO;
import com.atlas.auth.service.QrAuthService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/qr")
public class QrAuthController {

    private final QrAuthService qrAuthService;

    @GetMapping("/ticket")
    public Result<QrAuthTicketVO> ticket() {
        QrAuthTicketVO ticket = qrAuthService.ticket();
        return ResultGenerator.ok(ticket);
    }

    @PostMapping("/confirm")
    public Result<QrAuthStatusVO> confirm(@RequestParam("sceneId") String sceneId) {

        return ResultGenerator.ok();
    }

    @GetMapping("/status")
    public Result<QrAuthStatusVO> status(@RequestParam("sceneId") String sceneId) {
        QrAuthStatusVO status = qrAuthService.status(sceneId);
        return ResultGenerator.ok(status);
    }

}
