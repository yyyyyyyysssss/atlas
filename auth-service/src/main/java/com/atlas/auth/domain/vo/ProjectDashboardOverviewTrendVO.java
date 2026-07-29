package com.atlas.auth.domain.vo;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @Description 项目看板概览及趋势 VO
 * @Author ys
 * @Date 2026/7/28 14:09
 */
public record ProjectDashboardOverviewTrendVO(
        // 用户总数卡片（带环比/同比）
        UserCardMetric totalUser,

        // 当前活跃用户数卡片（带活跃率）
        ActiveUserCardMetric activeUser,

        // 活跃会话数卡片（基础数值+趋势）
        ActiveSessionCardMetric activeSession,

        // 累计授权次数卡片（基础数值+趋势）
        HistoryAuthCardMetric historyAuthorization
) {

    public static ProjectDashboardOverviewTrendVO of(
            ProjectDashboardMetricVO overview,
            List<ProjectDashboardMetricVO> trendList
    ) {
        // 安全处理 null
        ProjectDashboardMetricVO safeOverview = Optional.ofNullable(overview)
                .orElse(new ProjectDashboardMetricVO());

        List<ProjectDashboardMetricVO> safeTrendList = Optional.ofNullable(trendList)
                .orElse(Collections.emptyList());

        // 提取各指标的区间趋势数据
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

        // 计算日环比与周环比
        long totalUser = Optional.ofNullable(safeOverview.getTotalUserCount()).orElse(0L);
        GrowthRateResult dayGrowth = calculateGrowth(totalUser, safeOverview.getYesterdayTotalUserCount());
        GrowthRateResult weekGrowth = calculateGrowth(totalUser, safeOverview.getLastWeekTotalUserCount());

        // 计算用户活跃率
        long activeUserCount = Optional.ofNullable(safeOverview.getTotalActiveUserCount()).orElse(0L);
        String userActiveRate = "0.0%";
        if (totalUser > 0 && activeUserCount > 0) {
            double rate = ((double) activeUserCount / totalUser) * 100;
            userActiveRate = String.format("%.1f%%", rate);
        }

        // 计算人均会话数
        Long activeSessionCount = Optional.ofNullable(safeOverview.getTotalActiveSessionCount()).orElse(0L);
        String avgSessionPerUser = "0.0";
        if (activeUserCount > 0 && activeSessionCount > 0) {
            double avg = (double) activeSessionCount / activeUserCount;
            avgSessionPerUser = String.format("%.1f", avg);
        }


        // 组装返回：根据不同卡片类型注入对应的数据结构
        return new ProjectDashboardOverviewTrendVO(
                //  用户总数卡片
                new UserCardMetric(
                        Optional.ofNullable(safeOverview.getTotalUserCount()).orElse(0L),
                        totalUserTrend,
                        dayGrowth.text,
                        weekGrowth.text,
                        dayGrowth.positive,
                        weekGrowth.positive
                ),
                //  活跃用户卡片
                new ActiveUserCardMetric(
                        activeUserCount,
                        activeUserTrend,
                        userActiveRate
                ),
                //  活跃会话卡片
                new ActiveSessionCardMetric(
                        activeSessionCount,
                        activeSessionTrend,
                        avgSessionPerUser
                ),
                //  累计授权卡片（基础）
                new HistoryAuthCardMetric(
                        Optional.ofNullable(safeOverview.getTotalHistoryAuthorizationCount()).orElse(0L),
                        historyAuthTrend,
                        Optional.ofNullable(safeOverview.getLastWeekTotalAuthorizationCount()).orElse(0L)
                )
        );
    }


    private static GrowthRateResult calculateGrowth(long current, Long base) {
        if (base != null && base > 0) {
            double rate = ((double) (current - base) / base) * 100;
            String text = String.format("%s%.1f%%", rate >= 0 ? "+" : "", rate);
            return new GrowthRateResult(text, rate >= 0);
        }
        return new GrowthRateResult("+0.0%", true);
    }

    private record GrowthRateResult(String text, boolean positive) {}

    public static ProjectDashboardOverviewTrendVO empty() {
        return new ProjectDashboardOverviewTrendVO(
                UserCardMetric.empty(),
                ActiveUserCardMetric.empty(),
                ActiveSessionCardMetric.empty(),
                HistoryAuthCardMetric.empty()
        );
    }

    // 用户总数卡片
    public record UserCardMetric(
            Long value,
            List<Long> trend,
            String dayGrowth,
            String weekGrowth,
            Boolean dayPositive,
            Boolean weekPositive
    ) {
        public static UserCardMetric empty() {
            return new UserCardMetric(0L, Collections.emptyList(), "+0.0%", "+0.0%", true, true);
        }
    }

    // 活跃用户卡片
    public record ActiveUserCardMetric(
            Long value,
            List<Long> trend,
            String activeRate
    ) {
        public static ActiveUserCardMetric empty() {
            return new ActiveUserCardMetric(0L, Collections.emptyList(), "0.0%");
        }
    }

    // 活跃会话卡片
    public record ActiveSessionCardMetric(
            Long value,
            List<Long> trend,
            String avgSessionPerUser
    ) {
        public static ActiveSessionCardMetric empty() {
            return new ActiveSessionCardMetric(0L, Collections.emptyList(), "0.0");
        }
    }

    // 累计授权卡片
    public record HistoryAuthCardMetric(
            Long value,
            List<Long> trend,
            Long lastWeekTotalAuthorizationCount
    ) {
        public static HistoryAuthCardMetric empty() {
            return new HistoryAuthCardMetric(0L, Collections.emptyList(), 0L);
        }
    }

}