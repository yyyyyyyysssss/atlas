package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/10 9:15
 */
public record InitEmailDTO(
        @NotBlank(message = "邮箱不能为空")
        String email,

        @NotBlank(message = "验证码不为为空")
        String code
) {

}
