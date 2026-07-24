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

        Long id,
        String projectName,
        String projectCode,
        String description,
        ProjectStatus status,
        Long ownerId,
        String ownerName,
        Boolean builtin,
        LocalDateTime createTime,
        LocalDateTime updateTime

) {

    public static ProjectVO of(Project entity) {
        if (entity == null) {
            return null;
        }
        return new ProjectVO(
                entity.getId(),
                entity.getProjectName(),
                entity.getProjectCode(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getOwnerId(),
                entity.getOwnerName(),
                entity.getBuiltin(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }

}
