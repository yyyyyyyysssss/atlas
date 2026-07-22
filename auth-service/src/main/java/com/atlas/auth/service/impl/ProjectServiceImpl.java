package com.atlas.auth.service.impl;

import com.atlas.auth.domain.dto.ProjectQueryDTO;
import com.atlas.auth.domain.dto.ProjectSaveDTO;
import com.atlas.auth.domain.entity.Project;
import com.atlas.auth.domain.vo.ProjectCreateVO;
import com.atlas.auth.domain.vo.ProjectVO;
import com.atlas.auth.enums.ProjectStatus;
import com.atlas.auth.mapper.ProjectMapper;
import com.atlas.auth.service.ProjectService;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.common.mybatis.handler.DataPermissionContext;
import com.atlas.security.utils.SecureUidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;


/**
 * (Project)表服务实现类
 *
 * @author ys
 * @since 2026-07-09 11:35:27
 */
@Service("projectService")
@AllArgsConstructor
@Slf4j
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private ProjectMapper projectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectCreateVO saveProject(ProjectSaveDTO saveDTO) {
        // 新增
        if (saveDTO.id() == null) {
            Project project = saveDTO.toProject();
            project.setId(IdGen.genId());
            project.setProjectCode(SecureUidGenerator.generateUniqueHex(8));
            project.setStatus(ProjectStatus.ACTIVE);
            this.save(project);
            return new ProjectCreateVO(project.getId(),project.getProjectCode());
        } else {
            // 编辑
            Long id = saveDTO.id();
            Project project = this.getById(id);
            if(project == null){
                throw new BusinessException("项目不存在");
            }
            project.setProjectName(saveDTO.projectName());
            project.setDescription(saveDTO.description());
            project.setStatus(saveDTO.status());
            project.setOwnerId(saveDTO.ownerId());
            project.setOwnerName(saveDTO.ownerName());
            this.updateById(project);
            return new ProjectCreateVO(project.getId(),project.getProjectCode());
        }
    }

    @Override
    public ProjectVO getProjectDetail(Long id) {
        Objects.requireNonNull(id, "项目ID不能为空");
        Project project = this.getById(id);
        if (project == null) {
            return null;
        }
        return ProjectVO.of(project);
    }

    @Override
    public ProjectVO getByCode(String projectCode) {
        Objects.requireNonNull(projectCode, "项目编码不能为空");
        Project project = this.lambdaQuery()
                .eq(Project::getProjectCode, projectCode)
                .one();
        if (project == null) {
            return null;
        }
        return ProjectVO.of(project);
    }

    @Override
    public PageInfo<ProjectVO> getPage(ProjectQueryDTO queryDTO) {
        try (DataPermissionContext ctx = DataPermissionContext.open()){
            Integer pageNum = queryDTO.getPageNum();
            Integer pageSize = queryDTO.getPageSize();
            PageHelper.startPage(pageNum, pageSize);
            QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByAsc("create_time");
            List<Project> projects = projectMapper.selectList(queryWrapper);
            if(CollectionUtils.isEmpty(projects)){
                return PageInfo.emptyPageInfo();
            }
            PageInfo<Project> entityPageInfo = new PageInfo<>(projects);
            return entityPageInfo.convert(ProjectVO::of);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long id) {
        Objects.requireNonNull(id, "项目ID不能为空");

        Project project = this.getById(id);
        if (project == null) {
            throw new BusinessException("项目不存在或已被删除");
        }

        // 已经处于归档状态，避免重复操作
        if (ProjectStatus.ARCHIVED.equals(project.getStatus())) {
            throw new BusinessException("项目已处于归档状态，无需重复操作");
        }

        // 修改状态为归档（逻辑删除）
        project.setStatus(ProjectStatus.ARCHIVED);
        boolean updated = this.updateById(project);
        if (!updated) {
            throw new BusinessException("删除项目失败，请稍后重试");
        }
        log.info("项目成功归档(逻辑删除)，ID: {}, projectCode: {}", id, project.getProjectCode());
    }
}

