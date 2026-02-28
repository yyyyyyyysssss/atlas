package com.atlas.user.mapper;

import com.atlas.user.domain.entity.Organization;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (Organization)表数据库访问层
 *
 * @author ys
 * @since 2026-02-28 16:21:33
 */
@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {
    
}

