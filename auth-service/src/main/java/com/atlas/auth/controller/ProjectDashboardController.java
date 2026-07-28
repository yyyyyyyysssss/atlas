package com.atlas.auth.controller;

import com.atlas.auth.domain.vo.ProjectDashboardOverviewTrendVO;
import com.atlas.auth.service.ProjectService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
