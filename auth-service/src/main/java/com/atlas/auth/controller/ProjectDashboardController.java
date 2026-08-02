package com.atlas.auth.controller;

import com.atlas.auth.domain.vo.ProjectDashboardChartTrendVO;
import com.atlas.auth.domain.vo.ProjectDashboardGrantTypeStatsVO;
import com.atlas.auth.domain.vo.ProjectDashboardOverviewTrendVO;
import com.atlas.auth.service.ProjectService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/7/28 14:11
 */
@RestController
@RequestMapping("/project/{projectCode}/dashboard")
@RequiredArgsConstructor
public class ProjectDashboardController {

    private final ProjectService projectService;

    @GetMapping("/overview/trend")
    public Result<ProjectDashboardOverviewTrendVO> overviewAndTrend(@PathVariable("projectCode") String projectCode) {
        ProjectDashboardOverviewTrendVO trendVO = projectService.getOverviewAndTrend(projectCode);
        return ResultGenerator.ok(trendVO);
    }

    @GetMapping("/chart/trend")
    public Result<ProjectDashboardChartTrendVO> chartTrend(@PathVariable("projectCode") String projectCode,
                                                              @RequestParam("metricType") String metricType,
                                                              @RequestParam(required = false, name = "days") Integer days) {
        ProjectDashboardChartTrendVO chartTrend = projectService.getChartTrend(projectCode, metricType, days);
        return ResultGenerator.ok(chartTrend);
    }


    @GetMapping("/chart/trend")
    public Result<List<ProjectDashboardGrantTypeStatsVO>> grantTypeStats(@PathVariable("projectCode") String projectCode){
        List<ProjectDashboardGrantTypeStatsVO> grantTypeStats = projectService.getGrantTypeStats(projectCode);
        return ResultGenerator.ok(grantTypeStats);
    }

}
