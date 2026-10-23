package com.atlas.auth.service.impl;

import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.domain.dto.ApplicationQueryDTO;
import com.atlas.auth.domain.dto.ApplicationSaveDTO;
import com.atlas.auth.domain.entity.Application;
import com.atlas.auth.domain.entity.Project;
import com.atlas.auth.domain.vo.ApplicationVO;
import com.atlas.auth.domain.vo.ProjectVO;
import com.atlas.auth.mapper.ApplicationMapper;
import com.atlas.auth.mapping.ApplicationMapping;
import com.atlas.auth.service.ApplicationService;
import com.atlas.auth.service.ProjectService;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.common.mybatis.handler.DataPermissionContext;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;


@Service
@Slf4j
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application> implements ApplicationService {

    @Resource
    private ProjectService projectService;

    @Override
    public Application getActive(Long id) {
        Application application = this.lambdaQuery()
                .eq(Application::getId, id)
                .eq(Application::getEnabled, true)
                .one();
        if(application == null){
            return null;
        }
        Project project = projectService.getActive(application.getProjectId());
        if (project == null) {
            return null;
        }
        return application;
    }

    @Override
    public Application getRequiredByIdAndProjectId(Long id, Long projectId) {
        Application application = this.lambdaQuery()
                .eq(Application::getProjectId, projectId)
                .eq(Application::getId, id)
                .one();
        if(application == null){
            throw new AuthServiceException("应用不存在或不属于当前项目");
        }
        return application;
    }

    @Override
    public List<Application> findByProjectId(Long projectId) {
        List<Application> applications = this.lambdaQuery()
                .eq(Application::getProjectId, projectId)
                .list();
        if (CollectionUtils.isEmpty(applications)) {
            return Collections.emptyList();
        }
        return applications;
    }

    @Override
    public Long saveInfo(String projectCode, ApplicationSaveDTO saveDTO) {
        ProjectVO projectVO = projectService.getEnabledRequiredByCode(projectCode);
        Application application;
        if (saveDTO.id() == null) {
            application = ApplicationMapping.INSTANCE.toApplication(saveDTO);
            application.setId(IdGen.genId());
            application.setProjectId(projectVO.id());
            if (saveDTO.enabled() == null) {
                application.setEnabled(Boolean.TRUE);
            }
            this.save(application);
        } else {
            application = this.lambdaQuery()
                    .eq(Application::getProjectId, projectVO.id())
                    .eq(Application::getId, saveDTO.id())
                    .one();
            ApplicationMapping.INSTANCE.updateApplication(saveDTO, application);
            this.updateById(application);
        }
        return application.getId();
    }

    public boolean updateEnabled(String projectCode, Long id, Boolean enabled) {
        ProjectVO projectVO = projectService.getEnabledRequiredByCode(projectCode);
        return this.lambdaUpdate()
                .set(Application::getEnabled, enabled)
                .eq(Application::getProjectId, projectVO.id())
                .eq(Application::getId, id)
                .update();
    }

    @Override
    public PageInfo<ApplicationVO> getPage(String projectCode, ApplicationQueryDTO queryDTO) {
        ProjectVO projectVO = projectService.getByCode(projectCode);
        try (DataPermissionContext ctx = DataPermissionContext.open()) {
            Integer pageNum = queryDTO.getPageNum();
            Integer pageSize = queryDTO.getPageSize();
            PageHelper.startPage(pageNum, pageSize);
            List<Application> applications = this.lambdaQuery()
                    .eq(Application::getProjectId, projectVO.id())
                    .like(StringUtils.hasText(queryDTO.getApplicationName()), Application::getApplicationName, queryDTO.getApplicationName())
                    .orderByAsc(Application::getCreateTime)
                    .list();
            if (CollectionUtils.isEmpty(applications)) {
                return PageInfo.emptyPageInfo();
            }
            PageInfo<Application> entityPageInfo = new PageInfo<>(applications);
            return entityPageInfo.convert(ApplicationMapping.INSTANCE::toApplicationVO);
        }
    }

    @Override
    public ApplicationVO detail(String projectCode, Long applicationId) {
        ProjectVO projectVO = projectService.getByCode(projectCode);
        Application application = this.lambdaQuery()
                .eq(Application::getProjectId, projectVO.id())
                .eq(Application::getId, applicationId)
                .one();
        return ApplicationMapping.INSTANCE.toApplicationVO(application);
    }


}
