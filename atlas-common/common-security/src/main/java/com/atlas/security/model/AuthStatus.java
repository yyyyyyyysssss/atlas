package com.atlas.security.model;

public enum AuthStatus {

    /**
     * 认证成功，直接发令牌
     */
    SUCCESS,

    /**
     * 需要进行多因素认证（MFA），需凭 mfaTicket 走下一步
     */
    MFA_REQUIRED

}
