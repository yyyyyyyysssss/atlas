package com.atlas.auth.domain.entity;

import java.time.LocalDateTime;
import java.time.LocalDateTime;

import com.atlas.auth.enums.ProjectStatus;
import com.atlas.common.mybatis.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Tolerate;
import com.atlas.common.mybatis.entity.BaseIdEntity;

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

    // 所属组织 未来扩展 
    @TableField("org_id")
    private Long orgId;

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

