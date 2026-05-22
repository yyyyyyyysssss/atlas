package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/10 9:15
 */
public record ChangePasswordDTO(
        @NotBlank(message = "原密码不能为空")
        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 32, message = "新密码长度必须在6到32个字符之间")
        String newPassword
) {

}
