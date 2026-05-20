package com.atlas.auth.controller;

import com.atlas.auth.domain.dto.CaptchaSendDTO;
import com.atlas.auth.service.CaptchaFactory;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description
 * @Author ys
 * @Date 2026/3/18 13:50
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/captcha")
public class CaptchaController {

    private final CaptchaFactory captchaFactory;

    @PostMapping("/send")
    public Result<Void> send(@RequestBody @Validated CaptchaSendDTO captchaSendDTO) {
        captchaFactory.getService(captchaSendDTO.captchaType())
                .send(captchaSendDTO.target(), captchaSendDTO.captchaScene());
        return ResultGenerator.ok();
    }

}
