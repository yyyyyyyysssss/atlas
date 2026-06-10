package com.atlas.auth.domain.vo;

import com.atlas.auth.enums.Web3WalletType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/9 17:03
 */
@Builder
@Data
public class UserWeb3WalletVO {

    private Long id;

    private Long userId;

    private String address;

    private Web3WalletType walletType;

    private String source;

    private String label;

    private LocalDateTime createTime;

}
