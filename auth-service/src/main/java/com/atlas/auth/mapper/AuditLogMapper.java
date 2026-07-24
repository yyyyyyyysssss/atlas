package com.atlas.auth.mapper;

import com.atlas.auth.domain.entity.AuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (AuditLog)表数据库访问层
 *
 * @author ys
 * @since 2026-07-24 15:16:51
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
    
}

