package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/10 9:15
 */
public record ChangeUsernameDTO (

        @NotBlank(message = "账号不能为空")
        @Size(min = 5, max = 20, message = "账号长度必须在 5 到 20 个字符之间")
        @Pattern(
                regexp = "^[a-zA-Z0-9_]+$",
                message = "账号仅支持字母、数字和下划线"
        )
        String newUsername

){

}
