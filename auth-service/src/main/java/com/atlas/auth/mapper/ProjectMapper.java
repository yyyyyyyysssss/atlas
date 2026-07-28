package com.atlas.auth.mapper;

import com.atlas.auth.domain.entity.Project;
import com.atlas.auth.domain.vo.ProjectDashboardMetricVO;
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

}

