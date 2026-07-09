package com.atlas.auth.mapper;

import com.atlas.auth.domain.entity.Project;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (Project)表数据库访问层
 *
 * @author ys
 * @since 2026-07-09 11:35:27
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
    
}

