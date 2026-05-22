package com.atlas.auth.mapper;

import com.atlas.auth.domain.entity.UserPasswordCredentials;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (UserPasswordCredentials)表数据库访问层
 *
 * @author ys
 * @since 2026-05-22 09:09:13
 */
@Mapper
public interface UserPasswordCredentialsMapper extends BaseMapper<UserPasswordCredentials> {
    
}

