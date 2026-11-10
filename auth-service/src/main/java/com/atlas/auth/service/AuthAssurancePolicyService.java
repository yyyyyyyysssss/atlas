package com.atlas.auth.service;

import com.atlas.common.security.enums.AuthAssuranceLevel;
import com.atlas.common.security.model.SecurityUser;
import org.springframework.stereotype.Service;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/10 16:21
 */

@Service
public class AuthAssurancePolicyService {

    /**
     * 根据认证场景确定所需的安全等级
     */
    public AuthAssuranceLevel determineRequiredLevel(SecurityUser securityUser) {

        // 暂时使用 当前用户账号被期望的安全信任等级：开启了 MFA 就是 MEDIUM，没开启则是 LOW 后续可根据系统配置以及登录环境动态去判断
        return securityUser.isMfaEnabled() ? AuthAssuranceLevel.MEDIUM : AuthAssuranceLevel.LOW;
    }

}
