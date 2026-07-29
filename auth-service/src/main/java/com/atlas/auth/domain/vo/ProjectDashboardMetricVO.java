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

    /**
     * 近一周新增累计授权次数
     */
    private Long lastWeekTotalAuthorizationCount;


}
