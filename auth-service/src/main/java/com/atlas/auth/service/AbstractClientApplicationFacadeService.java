package com.atlas.auth.service;

import com.atlas.auth.domain.dto.ApplicationClientQueryDTO;
import com.atlas.auth.domain.entity.Application;
import com.atlas.auth.domain.vo.ApplicationClientVO;
import com.atlas.auth.domain.vo.ProjectVO;
import com.atlas.auth.enums.ClientProtocol;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/22 16:38
 */
public abstract class AbstractClientApplicationFacadeService {

    @Resource
    protected ProjectService projectService;

    @Resource
    protected ApplicationService applicationService;

    @Resource
    protected ApplicationClientService applicationClientService;


    public PageInfo<ApplicationClientVO> getPage(String projectCode, Long applicationId, ClientProtocol protocol, ApplicationClientQueryDTO queryDTO) {
        // 前置校验
        validateProjectAndApplication(projectCode, applicationId);

        Integer pageNum = queryDTO.getPageNum();
        Integer pageSize = queryDTO.getPageSize();

        return applicationClientService.getPage(applicationId, protocol, pageNum, pageSize);
    }

    /**
     * 校验项目是否存在，以及应用是否属于该项目。
     *
     * @param projectCode   项目编码
     * @param applicationId 应用ID
     */
    protected Application validateProjectAndApplication(String projectCode, Long applicationId) {
        ProjectVO project = projectService.getRequiredByCode(projectCode);
        return applicationService.getRequiredByIdAndProjectId(applicationId, project.id());
    }

}
