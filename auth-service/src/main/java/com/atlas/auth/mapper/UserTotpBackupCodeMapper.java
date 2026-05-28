package com.atlas.auth.mapper;

import com.atlas.auth.domain.entity.UserTotpBackupCode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (UserTotpBackupCode)表数据库访问层
 *
 * @author ys
 * @since 2026-05-28 14:22:20
 */
@Mapper
public interface UserTotpBackupCodeMapper extends BaseMapper<UserTotpBackupCode> {
    
}

