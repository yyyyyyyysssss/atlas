package com.atlas.auth.mapper;

import com.atlas.auth.domain.entity.UserTotpCredentials;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (UserTotpCredentials)表数据库访问层
 *
 * @author ys
 * @since 2026-05-27 14:56:07
 */
@Mapper
public interface UserTotpCredentialsMapper extends BaseMapper<UserTotpCredentials> {
    
}

