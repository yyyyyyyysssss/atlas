package com.atlas.auth.mapper;

import com.atlas.auth.domain.entity.UserGestureCredentials;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (UserGestureCredentials)表数据库访问层
 *
 * @author ys
 * @since 2026-06-04 08:58:22
 */
@Mapper
public interface UserGestureCredentialsMapper extends BaseMapper<UserGestureCredentials> {
    
}

