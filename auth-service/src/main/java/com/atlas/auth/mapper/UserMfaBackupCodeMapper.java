package com.atlas.auth.mapper;

import com.atlas.auth.domain.entity.UserMfaBackupCode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (UserMfaBackupCode)表数据库访问层
 *
 * @author ys
 * @since 2026-05-28 14:22:20
 */
@Mapper
public interface UserMfaBackupCodeMapper extends BaseMapper<UserMfaBackupCode> {
    
}

