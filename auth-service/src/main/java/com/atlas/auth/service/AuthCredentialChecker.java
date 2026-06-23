package com.atlas.auth.service;

import com.atlas.auth.enums.CredentialType;

public interface AuthCredentialChecker {

    /**
     * 获取当前策略对应的凭证类型
     */
    CredentialType getCredentialType();

    /**
     * 检查用户是否拥有该类型的任意有效凭证
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean hasCredential(Long userId);

    /**
     * 【核心】解绑专用：检查排除某个具体凭证 ID 后，用户是否还拥有该类型的其他凭证
     * 对于密码这种一对一的，直接返回 false；对于 WebAuthn/Web3 这种一对多的，排除当前 ID 后如果还有，返回 true
     * * @param userId 用户ID
     * @param credentialId 业务凭证唯一标识（可以是 Long 的 id，也可以是 WebAuthn 的 String 类型的 credential_id）
     */
    boolean hasCredentialExcluding(Long userId, Object credentialId);

}
