package com.atlas.auth.service;


import com.atlas.auth.domain.dto.ProjectQueryDTO;
import com.atlas.auth.domain.dto.ProjectSaveDTO;
import com.atlas.auth.domain.entity.Project;
import com.atlas.auth.domain.vo.ProjectCreateVO;
import com.atlas.auth.domain.vo.ProjectVO;
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

    // 分页条件查询项目列表
    PageInfo<ProjectVO> getPage(ProjectQueryDTO queryDTO);

    // 根据 ID 恢复项目
    void restoreProject(Long id);

    // 根据 ID 逻辑删除/归档项目
    void deleteProject(Long id);

    boolean isProjectActive(Long id);

    // 校验指定客户端所属的项目是否正常启用
    boolean isProjectActiveByRegisteredClientId(String registeredClientId);

}

