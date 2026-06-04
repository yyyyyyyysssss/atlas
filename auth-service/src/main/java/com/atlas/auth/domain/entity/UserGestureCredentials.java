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
 * (UserGestureCredentials)实体类
 *
 * @author ys
 * @since 2026-06-04 08:58:23
 */
@Getter
@Setter
@TableName(value = "user_gesture_credentials", autoResultMap = true)
@Builder
public class UserGestureCredentials extends BaseIdEntity {

    @Tolerate
    public UserGestureCredentials() {
    }

    // 用户ID 
    @TableField("user_id")
    private Long userId;

    // 加密后的手势 
    @TableField("gesture")
    private String gesture;

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


}

