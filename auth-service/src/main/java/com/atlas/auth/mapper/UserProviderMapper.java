package com.atlas.auth.mapper;


import com.atlas.auth.domain.entity.UserProvider;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (UserProvider)表数据库访问层
 *
 * @author ys
 * @since 2026-05-08 17:33:59
 */
@Mapper
public interface UserProviderMapper extends BaseMapper<UserProvider> {
    
}

