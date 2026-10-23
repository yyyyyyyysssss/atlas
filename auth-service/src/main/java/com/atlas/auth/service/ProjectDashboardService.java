package com.atlas.auth.service;


import com.atlas.auth.domain.entity.Application;
import com.atlas.auth.domain.entity.ApplicationClient;
import com.atlas.auth.domain.vo.*;
import com.atlas.auth.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectDashboardService {

    private final ProjectMapper projectMapper;

    private final ProjectService projectService;

    private final ApplicationService applicationService;

    private final ApplicationClientService applicationClientService;

    public ProjectDashboardOverviewTrendVO getOverviewAndTrend(String projectCode) {
        ProjectVO projectVO = projectService.getByCode(projectCode);
        List<String> registeredClientIds = this.getRegisteredClientIds(projectVO.id());
        if (CollectionUtils.isEmpty(registeredClientIds)) {
            return ProjectDashboardOverviewTrendVO.empty();
        }
        // 概览卡片汇总（当前实时值）
        ProjectDashboardMetricVO overviewMetric = projectMapper.selectOverviewByRegisteredClientIds(registeredClientIds);
        // 趋势明细列表：计算近 14 天的时间区间 [13天前, 今天]
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(13);
        List<ProjectDashboardMetricVO> trendMetrics = projectMapper.selectTrendByRegisteredClientIds(registeredClientIds, startDate, endDate);

        return ProjectDashboardOverviewTrendVO.of(overviewMetric, trendMetrics);
    }

    public ProjectDashboardChartTrendVO getChartTrend(String projectCode, String metricType, Integer days) {
        int queryDays = (days != null && days > 0) ? days : 30;
        ProjectVO projectVO = projectService.getByCode(projectCode);
        List<String> registeredClientIds = this.getRegisteredClientIds(projectVO.id());
        if (CollectionUtils.isEmpty(registeredClientIds)) {
            return ProjectDashboardChartTrendVO.empty(metricType);
        }
        List<ProjectDashboardChartTrendVO.TrendItem> trendItems;
        List<ProjectDashboardRankingVO> rankingList;
        switch (metricType) {
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

    public List<ProjectDashboardGrantTypeStatsVO> getGrantTypeStats(String projectCode) {
        ProjectVO projectVO = projectService.getByCode(projectCode);
        List<String> registeredClientIds = this.getRegisteredClientIds(projectVO.id());
        if (CollectionUtils.isEmpty(registeredClientIds)) {
            return Collections.emptyList();
        }
        List<Application> applications = applicationService.findByProjectId(projectVO.id());
        if (CollectionUtils.isEmpty(applications)) {
            return Collections.emptyList();
        }
        List<String> allApplicationNames = applications.stream().map(Application::getApplicationName).toList();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        List<ProjectDashboardGrantTypeStatsVO.GrantTypeItem> dbItems = projectMapper.selectGrantTypeStats(registeredClientIds, startDate, endDate);

        List<String> allGrantTypes = Arrays.asList("授权码模式", "客户端模式", "设备码模式");

        // key = "应用名_授权类型", value = count
        Map<String, Long> dbDataMap = dbItems.stream().collect(Collectors.toMap(
                item -> item.applicationName() + "_" + item.grantType(),
                ProjectDashboardGrantTypeStatsVO.GrantTypeItem::count,
                (k1, k2) -> k1
        ));
        // 双循环补全全量数据（应用 × 授权类型），即使为0也会生成一条记录
        List<ProjectDashboardGrantTypeStatsVO.GrantTypeItem> grantTypeItems = new ArrayList<>();
        for (String appName : allApplicationNames) {
            for (String grantType : allGrantTypes) {
                String key = appName + "_" + grantType;
                long count = dbDataMap.getOrDefault(key, 0L);
                grantTypeItems.add(ProjectDashboardGrantTypeStatsVO.GrantTypeItem.of(appName, grantType, count));
            }
        }

        List<ProjectDashboardGrantTypeStatsVO> result = new ArrayList<>();

        Map<String, Long> totalGrantTypeMap = grantTypeItems.stream()
                .collect(Collectors.groupingBy(
                        ProjectDashboardGrantTypeStatsVO.GrantTypeItem::grantType,
                        Collectors.summingLong(ProjectDashboardGrantTypeStatsVO.GrantTypeItem::count)
                ));

        List<ProjectDashboardGrantTypeStatsVO.GrantTypeItem> totalItems = totalGrantTypeMap.entrySet().stream()
                .map(entry -> ProjectDashboardGrantTypeStatsVO.GrantTypeItem.of("all", entry.getKey(), entry.getValue()))
                .toList();

        result.add(ProjectDashboardGrantTypeStatsVO.of("all", totalItems));

        Map<String, List<ProjectDashboardGrantTypeStatsVO.GrantTypeItem>> appGroupMap = grantTypeItems.stream()
                .collect(Collectors.groupingBy(ProjectDashboardGrantTypeStatsVO.GrantTypeItem::applicationName));

        appGroupMap.forEach((appName, items) -> {
            result.add(ProjectDashboardGrantTypeStatsVO.of(appName, items));
        });

        return result;
    }


    private List<String> getRegisteredClientIds(Long projectId){
        List<Application> applications = applicationService.findByProjectId(projectId);
        if(CollectionUtils.isEmpty(applications)){
            return Collections.emptyList();
        }
        List<Long> applicationIds = applications.stream().map(Application::getId).toList();
        List<ApplicationClient> applicationClients = applicationClientService.findByApplicationIds(applicationIds);
        if(CollectionUtils.isEmpty(applicationClients)){
            return Collections.emptyList();
        }
        return applicationClients.stream().map(ApplicationClient::getRegisteredClientId).toList();
    }

}
