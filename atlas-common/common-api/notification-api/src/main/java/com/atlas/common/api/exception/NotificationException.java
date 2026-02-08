package com.atlas.common.api.exception;

import com.atlas.common.core.response.IErrorCode;
import com.atlas.common.core.response.ResultCode;
import lombok.Getter;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/3 14:57
 */
@Getter
public class NotificationException extends RuntimeException{

    public final IErrorCode code;

    private final String detail;

    public NotificationException(String detail) {
        this(ResultCode.NOTIFY_BIZ_ERROR,detail,null);
    }

    public NotificationException(IErrorCode code) {
        this(code,null,null);
    }

    public NotificationException(Throwable cause) {
        this(ResultCode.NOTIFY_BIZ_ERROR,null,cause);
    }

    public NotificationException(IErrorCode code, String detail) {
        this(code,detail,null);
    }

    public NotificationException(IErrorCode code, String detail, Throwable cause) {
        super(code.getMessage() + (detail == null ? "" : " -> " + detail),cause);
        this.code = code;
        this.detail = detail;
    }

}
