package com.atlas.auth.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/21 16:58
 */
@Builder
@Data
public class UserProviderVO {

    private Long id;

    /**
     * 身份提供商标识
     */
    private String provider;

    /**
     * 是否已绑定
     */
    private Boolean isBound;

    private String label;

    private String boundName;

    private LocalDateTime createTime;



}
