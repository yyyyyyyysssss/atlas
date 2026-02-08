package com.atlas.common.mybatis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @Description
 * @Author ys
 * @Date 2025/5/17 11:30
 */
@Getter
@Setter
public class BaseEntity extends BaseIdEntity{

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    protected LocalDateTime createTime;

    @TableField(value = "creator_id",fill = FieldFill.INSERT)
    protected Long creatorId;

    @TableField(value = "creator_name",fill = FieldFill.INSERT)
    protected String creatorName;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    protected LocalDateTime updateTime;

    @TableField(value = "updater_id",fill = FieldFill.INSERT_UPDATE)
    protected Long updaterId;

    @TableField(value = "updater_name",fill = FieldFill.INSERT_UPDATE)
    protected String updaterName;

}
