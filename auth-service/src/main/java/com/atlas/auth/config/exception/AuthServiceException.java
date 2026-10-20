package com.atlas.auth.config.exception;

import com.atlas.common.core.exception.BaseException;
import com.atlas.common.core.response.IErrorCode;
import com.atlas.common.core.response.ResultCode;

/**
 * @Description 所有 auth-service 业务异常的统一父类。
 * @Author ys
 * @Date 2026/10/20 10:30
 */
public class AuthServiceException extends BaseException {

    public AuthServiceException(IErrorCode errorCode) {
        super(errorCode);
    }

    public AuthServiceException(IErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public AuthServiceException(IErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public AuthServiceException(IErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    public AuthServiceException(String message) {
        super(ResultCode.BIZ_ERROR, message);
    }

    public AuthServiceException(String message, Throwable cause) {
        super(ResultCode.BIZ_ERROR, message, cause);
    }

}
