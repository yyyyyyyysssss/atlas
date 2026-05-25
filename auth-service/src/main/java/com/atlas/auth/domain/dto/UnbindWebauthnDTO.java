package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/22 14:16
 */
public record UnbindWebauthnDTO(

        @NotBlank(message = "安全验证凭证缺失，请重新进行身份验证")
        String ticket,

        @NotBlank(message = "凭证id不能为空")
        String credentialId
){
}
