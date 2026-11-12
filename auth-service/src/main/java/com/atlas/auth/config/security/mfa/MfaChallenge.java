package com.atlas.auth.config.security.mfa;

import com.atlas.common.security.enums.AuthAssuranceLevel;
import com.atlas.common.security.enums.ClientType;
import lombok.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/8/26 17:34
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MfaChallenge {

    private String challengeId;

    private Long userId;

    private ClientType clientType;

    // 当前认证的等级
    private AuthAssuranceLevel currentLevel;

    // 本次认证要求达到的等级
    private AuthAssuranceLevel requiredLevel;

}
