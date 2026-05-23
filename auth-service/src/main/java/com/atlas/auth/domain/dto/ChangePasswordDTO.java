package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/10 9:15
 */
public record ChangePasswordDTO(

        @NotBlank(message = "安全验证凭证缺失，请重新进行身份验证")
        String ticket,

        @NotBlank(message = "新密码不能为空")
        String newPassword,

        @NotBlank(message = "确认密码不能为空")
        String confirmPassword
) {

}
