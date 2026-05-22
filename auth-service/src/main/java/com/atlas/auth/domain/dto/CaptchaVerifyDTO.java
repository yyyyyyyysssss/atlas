package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.CaptchaScene;
import com.atlas.auth.enums.CaptchaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/22 13:51
 */
public record CaptchaVerifyDTO(

        @NotBlank(message = "接收目标不能为空")
        String target,

        @NotNull(message = "验证码类型不能为空")
        CaptchaType captchaType,

        @NotNull(message = "业务场景不能为空")
        CaptchaScene captchaScene,

        @NotBlank(message = "验证码不能为空")
        String code
) {}
