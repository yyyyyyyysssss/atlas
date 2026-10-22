package com.atlas.auth.service;

import com.atlas.auth.domain.vo.ProjectVO;
import com.atlas.auth.enums.ProjectStatus;
import com.atlas.common.core.exception.BusinessException;
import jakarta.annotation.Resource;

import java.util.Objects;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/22 16:38
 */
public abstract class AbstractClientApplicationFacadeService {

    @Resource
    protected ProjectService projectService;


    protected ProjectVO checkAndEnsureProjectActive(String projectCode) {
        ProjectVO projectVO = checkAndGetProject(projectCode);
        if (!ProjectStatus.ACTIVE.equals(projectVO.status())) {
            throw new BusinessException("项目处于" + projectVO.status().getDescription() + "状态，无法进行修改操作");
        }
        return projectVO;
    }

    protected ProjectVO checkAndEnsureProjectActive(String projectCode, Long checkProjectId) {
        ProjectVO projectVO = checkAndGetProject(projectCode, checkProjectId);
        if (!ProjectStatus.ACTIVE.equals(projectVO.status())) {
            throw new BusinessException("项目处于" + projectVO.status().getDescription() + "状态，无法进行修改操作");
        }
        return projectVO;
    }

    protected ProjectVO checkAndGetProject(String projectCode, Long checkProjectId) {
        ProjectVO projectVO = checkAndGetProject(projectCode);
        if (!Objects.equals(projectVO.id(), checkProjectId)) {
            throw new BusinessException("项目信息不匹配，请重新刷新页面");
        }
        return projectVO;
    }

    protected ProjectVO checkAndGetProject(String projectCode) {
        Objects.requireNonNull(projectCode, "项目编码不能为空");
        ProjectVO projectVO = projectService.getByCode(projectCode);
        if (projectVO == null) {
            throw new BusinessException("该项目不存在: " + projectCode);
        }
        return projectVO;
    }

}
