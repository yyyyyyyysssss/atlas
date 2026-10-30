package com.atlas.auth.config.security.cas;

import com.atlas.auth.config.exception.AuthErrorCode;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/29 15:33
 */
@Getter
public class CasAuthenticationException extends AuthenticationException {

    private final AuthErrorCode authErrorCode;

    private final String service;

    public CasAuthenticationException(AuthErrorCode authErrorCode) {
        this(authErrorCode, authErrorCode.getMessage(), null, null);
    }

    public CasAuthenticationException(AuthErrorCode authErrorCode, String msg) {
        this(authErrorCode, msg, null, null);
    }

    public CasAuthenticationException(AuthErrorCode authErrorCode, String msg, String service) {
        this(authErrorCode, msg, service, null);
    }

    public CasAuthenticationException(AuthErrorCode authErrorCode, String msg, Throwable cause) {
        this(authErrorCode, msg, null, cause);
    }

    public CasAuthenticationException(AuthErrorCode authErrorCode, String msg, String service, Throwable cause) {
        super(msg, cause);
        this.authErrorCode = authErrorCode;
        this.service = service;
    }
}
