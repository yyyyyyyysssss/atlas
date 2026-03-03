package com.atlas.user.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserOrgDTO {

    private Long id;

    // 用户id
    @NotNull(message = "用户id不能为空")
    private Long userId;

    // 组织id 
    private Long orgId;

    // 岗位id 
    private Long posId;

    // 是否主归属: 1是 0否 
    private Boolean isMain;


}

