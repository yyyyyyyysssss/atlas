package com.atlas.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/10 9:15
 */
@Getter
@Setter
public class ChangeUsernameDTO {

    @NotBlank(message = "账号不能为空")
    @Size(min = 5, max = 20, message = "账号长度必须在 5 到 20 个字符之间")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "账号仅支持字母、数字和下划线"
    )
    private String username;

}
