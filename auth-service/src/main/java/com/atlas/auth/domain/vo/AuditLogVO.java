package com.atlas.auth.domain.vo;

import com.atlas.auth.domain.entity.AuditLog;

import java.time.LocalDateTime;

public record AuditLogVO(

        Long id,

        Long userId,

        String summary,

        String target,

        LocalDateTime createTime

) {

    public static AuditLogVO of(AuditLog auditLog){
        return new AuditLogVO(
                auditLog.getId(),
                auditLog.getUserId(),
                auditLog.getSummary(),
                auditLog.getTarget(),
                auditLog.getCreateTime()
        );
    }

}
