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
public class AuthorityUpdateDTO {

    @NotBlank(message = "权限ID不能为空")
    private String id;

    private String parentId;

    private String rootId;

    private String code;

    private String name;

    @Valid
    private List<AuthorityUrl> urls;

    private Integer sort;

}
