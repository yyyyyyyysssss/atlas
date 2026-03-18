package com.atlas.auth.controller;

import com.atlas.auth.domain.dto.EmailCodeDTO;
import com.atlas.auth.service.EmailVerificationService;
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
@RequestMapping("/code")
public class AuthCodeController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/send-email")
    public Result<Void> sendEmailCode(@RequestBody @Validated EmailCodeDTO emailCodeDTO) {
        emailVerificationService.send(emailCodeDTO.getEmail());
        return ResultGenerator.ok();
    }

}
