package com.atlas.auth.mapper;

import com.atlas.auth.domain.entity.Project;
import com.atlas.auth.domain.vo.ProjectDashboardChartTrendVO;
import com.atlas.auth.domain.vo.ProjectDashboardMetricVO;
import com.atlas.auth.domain.vo.ProjectDashboardRankingVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * (Project)表数据库访问层
 *
 * @author ys
 * @since 2026-07-09 11:35:27
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {


    ProjectDashboardMetricVO selectOverviewByRegisteredClientIds(@Param("registeredClientIds") Collection<String> registeredClientIds);

    /**
     * 根据客户端 ID 集合与时间范围，查询区间内的每日趋势明细
     *
     * @param registeredClientIds 客户端 ID 列表
     * @param startDate           开始日期 (YYYY-MM-DD)，可为空，默认 6 天前
     * @param endDate             结束日期 (YYYY-MM-DD)，可为空，默认 今天
     */
    List<ProjectDashboardMetricVO> selectTrendByRegisteredClientIds(
            @Param("registeredClientIds") Collection<String> registeredClientIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );



    /**
     * 查询每日授权次数趋势
     *
     * @param registeredClientIds 客户端 ID 集合
     * @param days                查询天数（不传则默认 30 天）
     * @return 趋势数据列表
     */
    List<ProjectDashboardChartTrendVO.TrendItem> selectDailyAuthCountTrend(
            @Param("registeredClientIds") Collection<String> registeredClientIds,
            @Param("days") Integer days
    );

    /**
     * 查询每日活跃用户数趋势 (DAU)
     *
     * @param registeredClientIds 客户端 ID 集合
     * @param days                查询天数（不传则默认 30 天）
     * @return 趋势数据列表
     */
    List<ProjectDashboardChartTrendVO.TrendItem> selectDailyActiveUserCountTrend(
            @Param("registeredClientIds") Collection<String> registeredClientIds,
            @Param("days") Integer days
    );


    /**
     * 查询应用活跃用户排行榜
     *
     * @param registeredClientIds 客户端 ID 列表
     * @param days                统计天数（可空，默认30天）
     * @param limit               返回条数限制（可空，默认10条）
     * @return 排行榜数据列表
     */
    List<ProjectDashboardRankingVO> selectApplicationActiveUserRanking(
            @Param("registeredClientIds") List<String> registeredClientIds,
            @Param("days") Integer days,
            @Param("limit") Integer limit
    );
    /**
     * 查询应用授权次数排行榜
     *
     * @param registeredClientIds 客户端 ID 列表
     * @param days                统计天数（可空，默认30天）
     * @param limit               返回条数限制（可空，默认10条）
     * @return 排行榜数据列表
     */
    List<ProjectDashboardRankingVO> selectApplicationAuthCountRanking(
            @Param("registeredClientIds") List<String> registeredClientIds,
            @Param("days") Integer days,
            @Param("limit") Integer limit
    );

}

