package com.atlas.auth.mapper;

import com.atlas.auth.domain.entity.UserIdentifier;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (UserIdentifier)表数据库访问层
 *
 * @author ys
 * @since 2026-05-18 09:56:57
 */
@Mapper
public interface UserIdentifierMapper extends BaseMapper<UserIdentifier> {
    
}

