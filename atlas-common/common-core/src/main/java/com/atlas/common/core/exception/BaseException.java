package com.atlas.common.core.exception;

import com.atlas.common.core.response.IErrorCode;
import lombok.Getter;

import java.util.Objects;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/11 9:36
 */
@Getter
public abstract class BaseException extends RuntimeException{

    private final IErrorCode errorCode;

    private final String detail;

    public BaseException(IErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public BaseException(IErrorCode errorCode, String detail) {
        this(errorCode, detail, null);
    }

    public BaseException(IErrorCode errorCode, Throwable cause) {
        this(errorCode, null, cause);
    }

    public BaseException(IErrorCode errorCode, String detail, Throwable cause) {
        // 构造父类消息：[错误信息] -> [详细详情]
        super(formatMessage(errorCode, detail), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.detail = detail;
    }

    private static String formatMessage(IErrorCode errorCode, String detail) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        String message = Objects.requireNonNull(errorCode.getMessage(), "errorCode.message must not be null");
        return detail == null || detail.isBlank()
                ? message
                : message + " -> " + detail;
    }

}
