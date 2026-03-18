package com.atlas.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2026/3/18 15:49
 */
@Getter
@Setter
public class EmailCodeDTO {

    @NotBlank(message = "邮箱不能为空")
    private String email;

}
