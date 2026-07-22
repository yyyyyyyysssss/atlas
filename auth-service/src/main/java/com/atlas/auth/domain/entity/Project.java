package com.atlas.auth.domain.entity;

import com.atlas.auth.enums.ProjectStatus;
import com.atlas.common.mybatis.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

/**
 * (Project)实体类
 *
 * @author ys
 * @since 2026-07-09 11:35:27
 */
@Getter
@Setter
@TableName(value = "project", autoResultMap = true)
@Builder
public class Project extends BaseEntity {

    @Tolerate
    public Project() {
    }

    // 项目名称 
    @TableField("project_name")
    private String projectName;

    // 项目唯一编码 
    @TableField("project_code")
    private String projectCode;

    // 项目简介 
    @TableField("description")
    private String description;

    // 状态: active(启用), suspended(暂停), archived(归档)
    @TableField("status")
    private ProjectStatus status;

    // 项目负责人 
    @TableField("owner_id")
    private Long ownerId;

    // 项目负责人名称 
    @TableField("owner_name")
    private String ownerName;


}

