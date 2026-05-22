package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/10 9:15
 */
public record ChangePasswordDTO(

        // "change" (密码验证) 或 "reset" (邮箱重置)
        @NotBlank(message = "验证方式不能为空")
        String verifyMethod,

        String code,

        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        String newPassword,

        @NotBlank(message = "确认密码不能为空")
        String confirmPassword
) {

}
