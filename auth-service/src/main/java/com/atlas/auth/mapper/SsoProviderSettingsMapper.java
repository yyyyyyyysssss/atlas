package com.atlas.auth.mapper;

import com.atlas.auth.domain.entity.SsoProviderSettings;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (SsoProviderSettings)表数据库访问层
 *
 * @author ys
 * @since 2026-06-17 09:08:23
 */
@Mapper
public interface SsoProviderSettingsMapper extends BaseMapper<SsoProviderSettings> {
    
}

