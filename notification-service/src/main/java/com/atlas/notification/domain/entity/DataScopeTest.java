package com.atlas.notification.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

/**
 * (DataScopeTest)实体类
 *
 * @author ys
 * @since 2026-04-14 09:07:01
 */
@Getter
@Setter
@TableName(value = "data_scope_test", autoResultMap = true)
@Builder
public class DataScopeTest{

    @Tolerate
    public DataScopeTest() {
    }

    @TableId(value = "id", type = IdType.INPUT)
    protected Long id;

    // 名称 
    @TableField("name")
    private String name;

    // 组织ID 
    @TableField("org_id")
    private Long orgId;

    @TableField("creator_id")
    protected Long creatorId;


}

