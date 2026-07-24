package com.atlas.auth.domain.entity;

import java.time.LocalDateTime;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Tolerate;
import com.atlas.common.mybatis.entity.BaseIdEntity;

/**
 * (AuditLog)实体类
 *
 * @author ys
 * @since 2026-07-24 15:16:51
 */
@Getter
@Setter
@TableName(value = "audit_log", autoResultMap = true)
@Builder
public class AuditLog extends BaseIdEntity {

    @Tolerate
    public AuditLog() {
    }

    @TableField("user_id")
    private Long userId;

    // 操作摘要 
    @TableField("summary")
    private String summary;

    // 目标对象 
    @TableField("target")
    private String target;

    @TableField("create_time")
    private LocalDateTime createTime;


}

