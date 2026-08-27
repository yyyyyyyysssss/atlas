package com.atlas.auth.config.security.mfa;

import com.atlas.security.enums.AuthAssuranceLevel;
import com.atlas.security.enums.ClientType;
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

    private String ticket;

    private Long userId;

    private ClientType clientType;

    /**
     * 本次认证要求达到的等级
     */
    private AuthAssuranceLevel requiredLevel;

}
