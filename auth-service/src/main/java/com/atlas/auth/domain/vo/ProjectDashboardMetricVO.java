package com.atlas.auth.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDashboardMetricVO {

    /**
     * 统计日期 (YYYY-MM-DD)
     * 概览查询时不查此列，默认映射为 null
     */
    private String statDate;

    /**
     * 累计用户总数
     */
    private Long totalUserCount;

    /**
     * 昨日结束累计总数
     */
    private Long yesterdayTotalUserCount;

    /**
     * 上周结束累计总数
     */
    private Long lastWeekTotalUserCount;

    /**
     * 活跃用户数
     */
    private Long totalActiveUserCount;

    /**
     * 活跃会话数
     */
    private Long totalActiveSessionCount;

    /**
     * 累计授权次数
     */
    private Long totalHistoryAuthorizationCount;

    private String dayOnDayGrowth;  // 日环比增长率 (例如 "+2.5%")
    private String weekOnWeekGrowth; // 周同比增长率 (例如 "+12.1%")
    private boolean dayPositive;     // 日环比是否为正（用于前端控制红绿颜色）
    private boolean weekPositive;    // 周同比是否为正

    public void calculateGrowthRates() {
        // 计算日环比：(今天总数 - 昨日总数) / 昨日总数 * 100
        if (yesterdayTotalUserCount != null && yesterdayTotalUserCount > 0) {
            double rate = ((double) (totalUserCount - yesterdayTotalUserCount) / yesterdayTotalUserCount) * 100;
            this.dayOnDayGrowth = String.format("%s%.1f%%", rate >= 0 ? "+" : "", rate);
            this.dayPositive = rate >= 0;
        } else {
            this.dayOnDayGrowth = "+0.0%";
            this.dayPositive = true;
        }

        // 计算周同比：(今天总数 - 上周同期总数) / 上周同期总数 * 100
        if (lastWeekTotalUserCount != null && lastWeekTotalUserCount > 0) {
            double rate = ((double) (totalUserCount - lastWeekTotalUserCount) / lastWeekTotalUserCount) * 100;
            this.weekOnWeekGrowth = String.format("%s%.1f%%", rate >= 0 ? "+" : "", rate);
            this.weekPositive = rate >= 0;
        } else {
            this.weekOnWeekGrowth = "+0.0%";
            this.weekPositive = true;
        }
    }


}
