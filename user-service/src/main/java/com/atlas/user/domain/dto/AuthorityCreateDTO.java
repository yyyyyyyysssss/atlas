package com.atlas.user.domain.dto;

import com.atlas.user.domain.entity.AuthorityUrl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2025/5/16 16:15
 */
@Getter
@Setter
public class AuthorityCreateDTO {

    @NotBlank(message = "关联菜单id不能为空")
    private String parentId;

    private String rootId;

    @NotBlank(message = "权限编码不能为空")
    private String code;

    @NotBlank(message = "权限名称不能为空")
    private String name;

    @Valid
    private List<AuthorityUrl> urls;

    private Integer sort;

}
