package com.atlas.user.domain.dto;

import com.atlas.user.enums.OrganizationStatus;
import com.atlas.user.enums.OrganizationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationCreateDTO {

    // 父节点id 
    private Long parentId;

    // 组织编码 
    private String orgCode;

    // 组织名称
    @NotBlank(message = "组织名称不能为空")
    private String orgName;

    // 状态 ENABLE: 启用 DISABLE: 停用 
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    // 组织类型 GROUP、COMPANY、DEPT、TEAM
    @NotNull(message = "组织类型不能为空")
    private OrganizationType orgType;

    // 组织路径 
    private String orgPath;

    // 组织路径名称 
    private String orgPathName;

    // 负责人id 
    private Long leaderId;

    // 排序 
    private Integer sort;

    // 备注 
    private String remark;


}

