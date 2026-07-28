package com.atlas.auth.domain.vo;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @Description
 * @Author ys
 * @Date 2026/7/28 14:09
 */
public record ProjectDashboardOverviewTrendVO(
        // 用户总数卡片（带环比/同比）
        CardMetric totalUser,

        // 当前活跃用户数卡片
        CardMetric activeUser,

        // 活跃会话数卡片
        CardMetric activeSession,

        // 累计授权次数卡片
        CardMetric historyAuthorization
) {

    public static ProjectDashboardOverviewTrendVO of(
            ProjectDashboardMetricVO overview,
            List<ProjectDashboardMetricVO> trendList
    ) {
        // 安全处理 null
        ProjectDashboardMetricVO safeOverview = Optional.ofNullable(overview)
                .orElse(new ProjectDashboardMetricVO());

        // 计算环比和同比
        safeOverview.calculateGrowthRates();

        List<ProjectDashboardMetricVO> safeTrendList = Optional.ofNullable(trendList)
                .orElse(Collections.emptyList());

        // 提取指标的区间趋势
        List<Long> totalUserTrend = safeTrendList.stream()
                .map(item -> Optional.ofNullable(item.getTotalUserCount()).orElse(0L))
                .toList();

        List<Long> activeUserTrend = safeTrendList.stream()
                .map(item -> Optional.ofNullable(item.getTotalActiveUserCount()).orElse(0L))
                .toList();

        List<Long> activeSessionTrend = safeTrendList.stream()
                .map(item -> Optional.ofNullable(item.getTotalActiveSessionCount()).orElse(0L))
                .toList();

        List<Long> historyAuthTrend = safeTrendList.stream()
                .map(item -> Optional.ofNullable(item.getTotalHistoryAuthorizationCount()).orElse(0L))
                .toList();

        // 组装返回：只有 totalUser 卡片带上环比/同比数据，其他卡片传默认值
        return new ProjectDashboardOverviewTrendVO(
                new CardMetric(
                        Optional.ofNullable(safeOverview.getTotalUserCount()).orElse(0L),
                        totalUserTrend,
                        safeOverview.getDayOnDayGrowth(),
                        safeOverview.getWeekOnWeekGrowth(),
                        safeOverview.isDayPositive(),
                        safeOverview.isWeekPositive()
                ),
                new CardMetric(Optional.ofNullable(safeOverview.getTotalActiveUserCount()).orElse(0L), activeUserTrend, "+0.0%", "+0.0%", true, true),
                new CardMetric(Optional.ofNullable(safeOverview.getTotalActiveSessionCount()).orElse(0L), activeSessionTrend, "+0.0%", "+0.0%", true, true),
                new CardMetric(Optional.ofNullable(safeOverview.getTotalHistoryAuthorizationCount()).orElse(0L), historyAuthTrend, "+0.0%", "+0.0%", true, true)
        );
    }

    public static ProjectDashboardOverviewTrendVO empty() {
        return new ProjectDashboardOverviewTrendVO(
                CardMetric.empty(),
                CardMetric.empty(),
                CardMetric.empty(),
                CardMetric.empty()
        );
    }

    public record CardMetric(
            Long value,
            List<Long> trend,
            String dayOnDayGrowth,
            String weekOnWeekGrowth,
            Boolean dayPositive,
            Boolean weekPositive
    ) {

        public static CardMetric empty() {
            return new CardMetric(0L, Collections.emptyList(), "+0.0%", "+0.0%", true, true);
        }

    }

}