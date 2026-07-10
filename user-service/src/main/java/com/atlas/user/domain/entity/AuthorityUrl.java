package com.atlas.user.domain.entity;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Tolerate;
import com.atlas.common.mybatis.entity.BaseIdEntity;

import java.util.List;

/**
 * (AuthorityUrl)实体类
 *
 * @author ys
 * @since 2026-07-10 11:11:01
 */
@Getter
@Setter
@TableName(value = "authority_url", autoResultMap = true)
@Builder
public class AuthorityUrl extends BaseIdEntity {

    @Tolerate
    public AuthorityUrl() {
    }

    // 权限id 
    @TableField("authority_id")
    private Long authorityId;

    // 请求路径 
    @TableField("url")
    private String url;

    // HTTP方法，ALL表示全部
    @TableField(value = "method", typeHandler = JacksonTypeHandler.class)
    private List<String> method;


}

