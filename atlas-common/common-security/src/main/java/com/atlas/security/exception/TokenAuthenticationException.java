package com.atlas.security.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/24 10:39
 */
public class TokenAuthenticationException extends AuthenticationException {

    public TokenAuthenticationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public TokenAuthenticationException(String msg) {
        super(msg);
    }
}
