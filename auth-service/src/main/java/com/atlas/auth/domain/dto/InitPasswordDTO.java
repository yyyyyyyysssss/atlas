package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/10 9:15
 */
public record InitPasswordDTO(
        @NotBlank(message = "密码不能为空")
        String password,

        @NotBlank(message = "确认密码不能为空")
        String confirmPassword
) {

}
