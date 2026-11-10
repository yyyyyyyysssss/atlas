package com.atlas.common.security.model;

import com.atlas.common.security.enums.AuthAssuranceLevel;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
        AuthStatus status,
        TokenInfo token,
        String mfaTicket,
        AuthAssuranceLevel requiredLevel,
        MfaType preferredMfaType,
        Set<MfaType> availableMfaTypes
) {

    /**
     * 快捷构建：登录成功响应
     */
    public static TokenResponse success(TokenInfo token) {
        return new TokenResponse(AuthStatus.SUCCESS, token, null,null, null, null);
    }

    public static TokenResponse success() {
        return new TokenResponse(AuthStatus.SUCCESS, null, null,null,null, null);
    }

    /**
     * 快捷构建：需要 MFA 验证响应
     */
    public static TokenResponse mfaRequired(String mfaTicket, AuthAssuranceLevel requiredLevel, MfaType preferredMfaType, Set<MfaType> availableMfaTypes) {

        return new TokenResponse(
                AuthStatus.MFA_REQUIRED,
                null,
                mfaTicket,
                requiredLevel,
                preferredMfaType,
                availableMfaTypes == null ? Set.of() : availableMfaTypes
        );
    }

}
