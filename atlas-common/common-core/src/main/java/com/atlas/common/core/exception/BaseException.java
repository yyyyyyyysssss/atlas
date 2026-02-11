package com.atlas.common.core.exception;

import com.atlas.common.core.response.IErrorCode;
import lombok.Getter;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/11 9:36
 */
@Getter
public abstract class BaseException extends RuntimeException{

    private final IErrorCode errorCode;

    private final String detail;

    protected BaseException(IErrorCode errorCode) {
        this(errorCode, null, null);
    }

    protected BaseException(IErrorCode errorCode, String detail) {
        this(errorCode, detail, null);
    }

    protected BaseException(IErrorCode errorCode, String detail, Throwable cause) {
        // 构造父类消息：[错误信息] -> [详细详情]
        super(formatMessage(errorCode, detail), cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    private static String formatMessage(IErrorCode errorCode, String detail) {
        if (errorCode == null) return "Unknown Error";
        return detail == null ? errorCode.getMessage() : errorCode.getMessage() + " -> " + detail;
    }

}
