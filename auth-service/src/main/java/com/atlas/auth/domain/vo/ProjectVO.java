package com.atlas.auth.domain.vo;

import com.atlas.auth.domain.entity.Project;
import com.atlas.auth.enums.ProjectStatus;

import java.time.LocalDateTime;

/**
 * @Description
 * @Author ys
 * @Date 2026/7/9 11:41
 */
public record ProjectVO (

        String id,
        String projectName,
        String projectCode,
        String description,
        Long orgId,
        ProjectStatus status,
        Long ownerId,
        String ownerName,
        LocalDateTime createTime,
        LocalDateTime updateTime

) {

    public static ProjectVO of(Project entity) {
        if (entity == null) {
            return null;
        }
        return new ProjectVO(
                String.valueOf(entity.getId()),
                entity.getProjectName(),
                entity.getProjectCode(),
                entity.getDescription(),
                entity.getOrgId(),
                entity.getStatus(),
                entity.getOwnerId(),
                entity.getOwnerName(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }

}
