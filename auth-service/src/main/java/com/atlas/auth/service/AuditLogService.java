package com.atlas.auth.service;


import com.atlas.auth.domain.dto.AuditLogQueryDTO;
import com.atlas.auth.domain.entity.AuditLog;
import com.atlas.auth.domain.vo.AuditLogVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;

/**
 * (AuditLog)表服务接口
 *
 * @author ys
 * @since 2026-07-24 15:16:51
 */
public interface AuditLogService extends IService<AuditLog> {


    PageInfo<AuditLogVO> pageLogs(AuditLogQueryDTO queryDTO);

}

