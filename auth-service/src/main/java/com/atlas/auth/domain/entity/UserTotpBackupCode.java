package com.atlas.auth.domain.entity;

import com.atlas.common.mybatis.entity.BaseIdEntity;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;

/**
 * (UserTotpBackupCode)实体类
 *
 * @author ys
 * @since 2026-05-28 14:22:20
 */
@Getter
@Setter
@TableName(value = "user_totp_backup_code", autoResultMap = true)
@Builder
public class UserTotpBackupCode extends BaseIdEntity {

    @Tolerate
    public UserTotpBackupCode() {
    }

    @TableField("user_id")
    private Long userId;

    // 恢复码 
    @TableField("backup_code")
    private String backupCode;

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    private LocalDateTime createTime;


}

