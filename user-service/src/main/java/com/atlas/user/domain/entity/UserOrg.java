package com.atlas.user.domain.entity;

import java.time.LocalDateTime;
import com.atlas.common.mybatis.entity.BaseIdEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Tolerate;


/**
 * (UserOrg)实体类
 *
 * @author ys
 * @since 2026-03-03 11:08:40
 */
@Getter
@Setter
@TableName(value = "user_org", autoResultMap = true)
@Builder
public class UserOrg extends BaseIdEntity {

    @Tolerate
    public UserOrg() {
    }

    // 用户id 
    @TableField("user_id")
    private Long userId;

    // 组织id 
    @TableField("org_id")
    private Long orgId;

    // 岗位id 
    @TableField("pos_id")
    private Long posId;

    // 加入时间 
    @TableField("join_time")
    private LocalDateTime joinTime;

    // 是否主归属: 1是 0否 
    @TableField("is_main")
    private Boolean isMain;


}

