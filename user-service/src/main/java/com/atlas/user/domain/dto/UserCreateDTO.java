package com.atlas.user.domain.dto;


import com.atlas.common.core.validation.OneNotBlank;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/3 15:39
 */
@Getter
@Setter
@OneNotBlank(value = {"email", "phone"}, message = "邮箱或手机号至少填写一个")
public class UserCreateDTO {

    @NotBlank(message = "账号不能为空")
    private String username;

    private String password;

    private String fullName;

    private String email;

    private String avatar;

    private String phone;

    private boolean enabled = true;

    private List<Long> roleIds;


}
