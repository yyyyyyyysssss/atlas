package com.atlas.auth.config.security.cas;

import com.atlas.auth.config.exception.AuthErrorCode;
import lombok.Getter;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/29 17:35
 */
@Getter
public class CasAuthenticationRequiredException extends CasAuthenticationException{

    private String casRequestId;

    public CasAuthenticationRequiredException(String casRequestId) {
        super(AuthErrorCode.CAS_AUTHENTICATION_REQUIRED);
        this.casRequestId = casRequestId;
    }
}
