package com.atlas.auth.service.impl;

import com.atlas.auth.domain.dto.AuditLogQueryDTO;
import com.atlas.auth.domain.entity.AuditLog;
import com.atlas.auth.domain.vo.AuditLogVO;
import com.atlas.auth.event.AuditLogEvent;
import com.atlas.auth.mapper.AuditLogMapper;
import com.atlas.auth.service.AuditLogService;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.common.mybatis.handler.DataPermissionContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;


/**
 * (AuditLog)表服务实现类
 *
 * @author ys
 * @since 2026-07-24 15:16:51
 */
@Service("auditLogService")
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Async
    @EventListener
    public void onAuditLogEvent(AuditLogEvent event) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setId(IdGen.genId());
            auditLog.setUserId(event.userId());
            auditLog.setSummary(event.summary());
            auditLog.setTarget(event.target());
            auditLog.setCreateTime(LocalDateTime.now());
            this.save(auditLog);
        } catch (Exception e) {
            log.error("保存审计日志失败, event: {}", event, e);
        }
    }

    @Override
    public PageInfo<AuditLogVO> pageLogs(AuditLogQueryDTO queryDTO) {
        try (DataPermissionContext ctx = DataPermissionContext.open()) {
            ctx.configure(consumer -> consumer.setUserField("user_id"));
            Integer pageNum = queryDTO.getPageNum();
            Integer pageSize = queryDTO.getPageSize();
            PageHelper.startPage(pageNum, pageSize);
            LambdaQueryWrapper<AuditLog> queryWrapper = new LambdaQueryWrapper<AuditLog>()
                    .eq(queryDTO.getUserId() != null, AuditLog::getUserId, queryDTO.getUserId())
                    .orderByDesc(AuditLog::getCreateTime);
            List<AuditLog> auditLogs = auditLogMapper.selectList(queryWrapper);
            return new PageInfo<>(auditLogs).convert(AuditLogVO::of);
        }
    }
}

