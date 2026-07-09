package com.atlas.auth.domain.dto;

import com.atlas.auth.domain.entity.Project;
import com.atlas.auth.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;

/**
 * @Description
 * @Author ys
 * @Date 2026/7/9 11:47
 */
public record ProjectSaveDTO (
        Long id,

        @NotBlank(message = "项目名称不能为空")
        String projectName,

        ProjectStatus status,

        String description,

        Long orgId,

        Long ownerId,

        String ownerName
){

    public Project toProject(){
        Project project = new Project();
        project.setProjectName(projectName);
        project.setStatus(status);
        project.setDescription(description);
        project.setOrgId(orgId);
        project.setOwnerId(ownerId);
        project.setOwnerName(ownerName);

        return project;
    }

}
