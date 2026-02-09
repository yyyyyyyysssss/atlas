package com.atlas.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class UserUpdateDTO {

    @NotNull(message = "id不能为空")
    private Long id;

    private String password;

    @NotBlank(message = "用户名不能为空",groups = UpdateAll.class)
    private String fullName;

    private String avatar;

    private String email;

    private String phone;

    private Boolean enabled;

    private List<Long> roleIds;

    public interface UpdateAll{}


}
