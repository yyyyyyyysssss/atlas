package com.atlas.user.domain.dto;

import com.atlas.user.domain.entity.UserSetting;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/10 9:15
 */
@Getter
@Setter
public class UserProfileDTO {

    @Size(max = 20, message = "姓名长度不能超过20个字符")
    private String fullName;

    @URL(message = "头像地址格式不正确")
    @Size(max = 255, message = "头像路径过长")
    private String avatar;

    @Size(max = 100, message = "座右铭不能超过100个字符")
    private String motto;

    @Valid
    private UserSetting settings;

}
