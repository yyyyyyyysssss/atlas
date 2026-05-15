package com.atlas.auth.mapper;


import com.atlas.auth.domain.entity.UserIdentity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (UserIdentity)表数据库访问层
 *
 * @author ys
 * @since 2026-05-08 17:33:59
 */
@Mapper
public interface UserIdentityMapper extends BaseMapper<UserIdentity> {
    
}

