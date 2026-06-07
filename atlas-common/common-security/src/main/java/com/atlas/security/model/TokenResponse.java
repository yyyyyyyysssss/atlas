package com.atlas.security.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
        AuthStatus status,
        TokenInfo token,
        String mfaTicket,
        MfaType mfaType,
        Set<MfaType> activeMfaStrategies
) {

    /**
     * 快捷构建：登录成功响应
     */
    public static TokenResponse success(TokenInfo token) {
        return new TokenResponse(AuthStatus.SUCCESS, token, null,null, null);
    }

    /**
     * 快捷构建：需要 MFA 验证响应
     */
    public static TokenResponse mfaRequired(String mfaTicket, MfaType mfaType, Set<MfaType> activeMfaStrategies) {
        return new TokenResponse(AuthStatus.MFA_REQUIRED, null, mfaTicket, mfaType, activeMfaStrategies);
    }

}
