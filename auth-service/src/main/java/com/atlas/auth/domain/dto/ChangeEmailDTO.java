package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/22 14:16
 */
public record ChangeEmailDTO (
        @NotBlank(message = "邮箱不能为空")
        String newEmail,

        @NotBlank(message = "验证码不为为空")
        String code
){
}
