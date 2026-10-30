package com.atlas.auth.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/30 10:49
 */

@Getter
@Setter
@TableName(value = "cas_registered_client", autoResultMap = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CasRegisteredClient {


    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("client_name")
    private String clientName;

    @TableField("service")
    private String service;

    @TableField("service_hash")
    private String serviceHash;

}
