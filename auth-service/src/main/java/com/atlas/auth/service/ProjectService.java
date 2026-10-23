package com.atlas.auth.service;


import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.domain.dto.ProjectQueryDTO;
import com.atlas.auth.domain.dto.ProjectSaveDTO;
import com.atlas.auth.domain.entity.Project;
import com.atlas.auth.domain.vo.*;
import com.atlas.auth.enums.ProjectStatus;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * (Project)表服务接口
 *
 * @author ys
 * @since 2026-07-09 11:35:27
 */
public interface ProjectService extends IService<Project> {


    // 保存或更新项目 (根据 ID 自动判定)
    ProjectCreateVO saveProject(ProjectSaveDTO saveDTO);

    ProjectVO getProjectDetail(Long id);

    // 根据项目唯一编码获取项目详情
    ProjectVO getByCode(String projectCode);

    default ProjectVO getRequiredByCode(String projectCode){
        ProjectVO projectVO = getByCode(projectCode);
        if(projectVO == null){
            throw new AuthServiceException("该项目不存在: " + projectCode);
        }
        return projectVO;
    }

    default ProjectVO getEnabledRequiredByCode(String projectCode){
        ProjectVO projectVO = getRequiredByCode(projectCode);
        if(!ProjectStatus.ACTIVE.equals(projectVO.status())){
            throw new AuthServiceException("该项目处于" + projectVO.status().getDescription() + "状态，无法进行修改操作");
        }
        return projectVO;
    }

    // 分页条件查询项目列表
    PageInfo<ProjectVO> getPage(ProjectQueryDTO queryDTO);

    // 根据 ID 恢复项目
    void restoreProject(Long id);

    // 根据 ID 逻辑删除/归档项目
    void deleteProject(Long id);

    boolean isProjectActive(Long id);

    // 校验指定客户端所属的项目是否正常启用
    boolean isProjectActiveByRegisteredClientId(String registeredClientId);

    ProjectDashboardOverviewTrendVO getOverviewAndTrend(String projectCode);

    ProjectDashboardChartTrendVO getChartTrend(String projectCode, String metricType, Integer days);

    List<ProjectDashboardGrantTypeStatsVO> getGrantTypeStats(String projectCode);

}

