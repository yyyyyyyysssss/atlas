package com.atlas.user.domain.entity;

import com.atlas.common.mybatis.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

/**
 * (Organization)实体类
 *
 * @author ys
 * @since 2026-02-28 16:21:34
 */
@Getter
@Setter
@TableName(value = "organization", autoResultMap = true)
@Builder
public class Organization extends BaseEntity {

    @Tolerate
    public Organization() {
    }

    // 父节点id 
    @TableField("parent_id")
    private Long parentId;

    // 组织编码 
    @TableField("org_code")
    private String orgCode;

    // 组织名称 
    @TableField("org_name")
    private String orgName;

    // 状态 ENABLE: 启用 DISABLE: 停用 
    @TableField("status")
    private String status;

    // 组织类型 GROUP、COMPANY、DEPT、TEAM 
    @TableField("org_type")
    private String orgType;

    // 组织路径 
    @TableField("org_path")
    private String orgPath;

    // 组织路径名称 
    @TableField("org_path_name")
    private String orgPathName;

    // 负责人id 
    @TableField("leader_id")
    private Long leaderId;

    // 排序 
    @TableField("sort")
    private Integer sort;

    // 备注 
    @TableField("remark")
    private String remark;


}

