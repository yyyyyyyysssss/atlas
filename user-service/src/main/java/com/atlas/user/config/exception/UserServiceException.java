package com.atlas.user.config.exception;

import com.atlas.common.core.exception.BaseException;
import com.atlas.common.core.response.IErrorCode;
import com.atlas.common.core.response.ResultCode;

/**
 * @Description 所有 user-service 业务异常的统一父类。
 * @Author ys
 * @Date 2026/10/20 10:30
 */
public class UserServiceException extends BaseException {

    public UserServiceException(IErrorCode errorCode) {
        super(errorCode);
    }

    public UserServiceException(IErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public UserServiceException(IErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public UserServiceException(IErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    public UserServiceException(String message) {
        super(ResultCode.BIZ_ERROR, message);
    }

    public UserServiceException(String message, Throwable cause) {
        super(ResultCode.BIZ_ERROR, message, cause);
    }

}
