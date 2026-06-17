package com.atlas.auth.mapper;

import com.atlas.auth.domain.entity.SsoProvider;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (SsoProvider)表数据库访问层
 *
 * @author ys
 * @since 2026-06-17 09:06:51
 */
@Mapper
public interface SsoProviderMapper extends BaseMapper<SsoProvider> {
    
}

