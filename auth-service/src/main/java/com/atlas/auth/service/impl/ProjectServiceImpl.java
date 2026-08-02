package com.atlas.auth.service.impl;

import com.atlas.auth.domain.dto.ProjectQueryDTO;
import com.atlas.auth.domain.dto.ProjectSaveDTO;
import com.atlas.auth.domain.entity.OAuth2ClientApplication;
import com.atlas.auth.domain.entity.Project;
import com.atlas.auth.domain.vo.*;
import com.atlas.auth.enums.ProjectStatus;
import com.atlas.auth.event.AuditLogEvent;
import com.atlas.auth.mapper.ProjectMapper;
import com.atlas.auth.service.OAuth2ClientApplicationService;
import com.atlas.auth.service.ProjectService;
import com.atlas.common.core.context.UserContext;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.common.mybatis.handler.DataPermissionContext;
import com.atlas.security.utils.SecureUidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


/**
 * (Project)表服务实现类
 *
 * @author ys
 * @since 2026-07-09 11:35:27
 */
@Service("projectService")
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final ProjectMapper projectMapper;

    private final OAuth2ClientApplicationService oAuth2ClientApplicationService;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectCreateVO saveProject(ProjectSaveDTO saveDTO) {
        Long userId = UserContext.getRequiredUserId();
        // 新增
        if (saveDTO.id() == null) {
            Project project = saveDTO.toProject();
            project.setId(IdGen.genId());
            project.setProjectCode(SecureUidGenerator.generateUniqueHex(8));
            project.setStatus(ProjectStatus.ACTIVE);
            this.save(project);

            // 发布审计日志
            eventPublisher.publishEvent(new AuditLogEvent(userId, "创建项目", "project"));

            return new ProjectCreateVO(project.getId(),project.getProjectCode());
        } else {
            // 编辑
            Long id = saveDTO.id();
            Project project = this.getById(id);
            if(project == null){
                throw new BusinessException("项目不存在");
            }
            // 如果是内置项目，强行校验/修正状态，不允许改为 ARCHIVED 或其他禁用状态
            if(Boolean.TRUE.equals(project.getBuiltin())){
                if (saveDTO.status() != null && !ProjectStatus.ACTIVE.equals(saveDTO.status())) {
                    throw new BusinessException("内置项目状态必须保持启用，不允许修改为非激活状态");
                }
            } else {
                project.setStatus(saveDTO.status());
            }
            project.setProjectName(saveDTO.projectName());
            project.setDescription(saveDTO.description());
            project.setOwnerId(saveDTO.ownerId());
            project.setOwnerName(saveDTO.ownerName());
            this.updateById(project);

            // 发布审计日志
            eventPublisher.publishEvent(new AuditLogEvent(userId, "修改项目", "project"));

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
            if(queryDTO.getStatus() != null){
                queryWrapper.eq("status", queryDTO.getStatus());
            }
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
    public void restoreProject(Long id){
        Objects.requireNonNull(id, "项目ID不能为空");
        Project project = this.getById(id);
        if (project == null) {
            throw new BusinessException("项目不存在或已被删除");
        }
        if (Boolean.TRUE.equals(project.getBuiltin())) {
            throw new BusinessException("内置项目为系统基础数据，无需执行恢复操作");
        }
        // 已经处于启用状态，避免重复操作
        if (ProjectStatus.ACTIVE.equals(project.getStatus())) {
            throw new BusinessException("项目已处于启用状态，无需重复操作");
        }
        // 修改状态为启用
        project.setStatus(ProjectStatus.ACTIVE);
        boolean updated = this.updateById(project);
        if (!updated) {
            throw new BusinessException("恢复项目失败，请稍后重试");
        }
        log.info("项目成功恢复，ID: {}, projectCode: {}", id, project.getProjectCode());

        // 发布审计日志
        eventPublisher.publishEvent(new AuditLogEvent(UserContext.getRequiredUserId(), "恢复项目", "project"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long id) {
        Objects.requireNonNull(id, "项目ID不能为空");

        Project project = this.getById(id);
        if (project == null) {
            throw new BusinessException("项目不存在或已被删除");
        }

        if(Boolean.TRUE.equals(project.getBuiltin())){
            throw new BusinessException("系统内置项目，禁止进行删除或归档操作");
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

        // 发布审计日志
        eventPublisher.publishEvent(new AuditLogEvent(UserContext.getRequiredUserId(), "删除项目", "project"));
    }

    @Override
    public boolean isProjectActive(Long id){
        if (id == null) {
            return false;
        }
        Project project = this.getById(id);
        if (project == null) {
            log.warn("项目状态校验失败：项目不存在，ID: {}", id);
            return false;
        }
        boolean active = ProjectStatus.ACTIVE.equals(project.getStatus());
        if (!active) {
            log.warn("项目状态校验失败：项目未处于启用状态，ID: {}, status: {}", id, project.getStatus());
        }
        return active;
    }

    @Override
    public boolean isProjectActiveByRegisteredClientId(String registeredClientId) {
        if(!StringUtils.hasText(registeredClientId)){
            return false;
        }
        OAuth2ClientApplication auth2ClientApplication = oAuth2ClientApplicationService.findByRegisteredClientId(registeredClientId);
        if(auth2ClientApplication == null){
            return false;
        }
        return isProjectActive(auth2ClientApplication.getProjectId());
    }

    @Override
    public ProjectDashboardOverviewTrendVO getOverviewAndTrend(String projectCode) {
        ProjectVO projectVO = this.getByCode(projectCode);
        List<String> registeredClientIds= oAuth2ClientApplicationService.findRegisteredClientIdByProjectId(projectVO.id());
        if(CollectionUtils.isEmpty(registeredClientIds)){
            return ProjectDashboardOverviewTrendVO.empty();
        }
        // 概览卡片汇总（当前实时值）
        ProjectDashboardMetricVO overviewMetric = projectMapper.selectOverviewByRegisteredClientIds(registeredClientIds);
        // 趋势明细列表：计算近 14 天的时间区间 [13天前, 今天]
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(13);
        List<ProjectDashboardMetricVO> trendMetrics= projectMapper.selectTrendByRegisteredClientIds(registeredClientIds, startDate, endDate);

        return ProjectDashboardOverviewTrendVO.of(overviewMetric, trendMetrics);
    }

    @Override
    public ProjectDashboardChartTrendVO getChartTrend(String projectCode, String metricType, Integer days) {
        int queryDays = (days != null && days > 0) ? days : 30;
        ProjectVO projectVO = this.getByCode(projectCode);
        List<String> registeredClientIds= oAuth2ClientApplicationService.findRegisteredClientIdByProjectId(projectVO.id());
        if(CollectionUtils.isEmpty(registeredClientIds)){
            return ProjectDashboardChartTrendVO.empty(metricType);
        }
        List<ProjectDashboardChartTrendVO.TrendItem> trendItems;
        List<ProjectDashboardRankingVO> rankingList;
        switch (metricType){
            case "ACTIVE_USER":
                trendItems = projectMapper.selectDailyActiveUserCountTrend(registeredClientIds, queryDays);
                rankingList = projectMapper.selectApplicationActiveUserRanking(registeredClientIds, days, 6);
                break;
            case "AUTH_COUNT":
                trendItems = projectMapper.selectDailyAuthCountTrend(registeredClientIds, queryDays);
                rankingList = projectMapper.selectApplicationAuthCountRanking(registeredClientIds, days, 6);
                break;
            default:
                return ProjectDashboardChartTrendVO.empty(metricType);
        }

        return ProjectDashboardChartTrendVO.of(metricType, trendItems, rankingList);
    }

    @Override
    public List<ProjectDashboardGrantTypeStatsVO> getGrantTypeStats(String projectCode) {
        ProjectVO projectVO = this.getByCode(projectCode);
        List<String> registeredClientIds= oAuth2ClientApplicationService.findRegisteredClientIdByProjectId(projectVO.id());
        if(CollectionUtils.isEmpty(registeredClientIds)){
            return Collections.emptyList();
        }
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        List<ProjectDashboardGrantTypeStatsVO.GrantTypeItem> grantTypeItems = projectMapper.selectGrantTypeStats(registeredClientIds, startDate, endDate);

        List<ProjectDashboardGrantTypeStatsVO> result = new ArrayList<>();

        Map<String, Long> totalGrantTypeMap = grantTypeItems.stream()
                .collect(Collectors.groupingBy(
                        ProjectDashboardGrantTypeStatsVO.GrantTypeItem::grantType,
                        Collectors.summingLong(ProjectDashboardGrantTypeStatsVO.GrantTypeItem::count)
                ));

        List<ProjectDashboardGrantTypeStatsVO.GrantTypeItem> totalItems = totalGrantTypeMap.entrySet().stream()
                .map(entry -> ProjectDashboardGrantTypeStatsVO.GrantTypeItem.of("全部应用", entry.getKey(), entry.getValue()))
                .toList();

        result.add(ProjectDashboardGrantTypeStatsVO.of("全部应用", totalItems));

        Map<String, List<ProjectDashboardGrantTypeStatsVO.GrantTypeItem>> appGroupMap = grantTypeItems.stream()
                .collect(Collectors.groupingBy(ProjectDashboardGrantTypeStatsVO.GrantTypeItem::applicationName));

        appGroupMap.forEach((appName, items) -> {
            result.add(ProjectDashboardGrantTypeStatsVO.of(appName, items));
        });

        return result;
    }
}

