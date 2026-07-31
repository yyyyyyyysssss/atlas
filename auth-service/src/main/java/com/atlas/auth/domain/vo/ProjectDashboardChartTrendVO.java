package com.atlas.auth.domain.vo;

import java.util.Collections;
import java.util.List;

public record ProjectDashboardChartTrendVO(

        // 指标类型 ACTIVE_USER: 活跃用户, AUTH_COUNT: 授权次数）
        String metricType,

        List<TrendItem> trendList,

        List<ProjectDashboardRankingVO> rankingList

) {

    public static ProjectDashboardChartTrendVO of(String metricType, List<TrendItem> trendItems, List<ProjectDashboardRankingVO> rankingList) {
        return new ProjectDashboardChartTrendVO(metricType, trendItems, rankingList);
    }

    public static ProjectDashboardChartTrendVO empty(String metricType) {
        return new ProjectDashboardChartTrendVO(metricType, Collections.emptyList(), Collections.emptyList());
    }

    public record TrendItem(
            String letter,
            Long frequency
    ) {

    }

}
