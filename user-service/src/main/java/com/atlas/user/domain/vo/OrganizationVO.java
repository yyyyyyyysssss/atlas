package com.atlas.user.domain.vo;

import com.atlas.user.enums.OrganizationStatus;
import com.atlas.user.enums.OrganizationType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrganizationVO {


    private Long id;

    // 父节点id 
    private Long parentId;

    private String parentCode;

    private String parentName;

    // 组织编码 
    private String orgCode;

    // 组织名称 
    private String orgName;

    // 状态 ENABLE: 启用 DISABLE: 停用 
    private OrganizationStatus status;

    // 组织类型 GROUP、COMPANY、DEPT、TEAM 
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

    private String createTime;

    private Long creatorId;

    private String creatorName;

    private String updateTime;

    private Long updaterId;

    private String updaterName;

    private List<OrganizationVO> children;

    public String getStatusName() {
        return status != null ? status.getDescription() : null;
    }

    public String getOrgTypeName() {
        return orgType != null ? orgType.getDescription() : null;
    }
}

