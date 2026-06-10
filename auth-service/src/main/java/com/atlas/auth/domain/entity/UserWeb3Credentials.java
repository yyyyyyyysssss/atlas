package com.atlas.auth.domain.entity;

import java.time.LocalDateTime;
import java.time.LocalDateTime;

import com.atlas.auth.enums.Web3WalletType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Tolerate;
import com.atlas.common.mybatis.entity.BaseIdEntity;

/**
 * (UserWeb3Credentials)实体类
 *
 * @author ys
 * @since 2026-06-09 16:45:48
 */
@Getter
@Setter
@TableName(value = "user_web3_credentials", autoResultMap = true)
@Builder
public class UserWeb3Credentials extends BaseIdEntity {

    @Tolerate
    public UserWeb3Credentials() {
    }

    // 用户id 
    @TableField("user_id")
    private Long userId;

    // 区块链生态: ETHEREUM, BITCOIN, SOLANA 
    @TableField("chain_type")
    private String chainType;

    // 地址
    @TableField("address")
    private String address;

    // 签名协议类型: EOA, EIP712, BIP322 
    @TableField("wallet_type")
    private Web3WalletType walletType;

    @TableField("label")
    private String label;

    // 来源 
    @TableField("source")
    private String source;

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


}

