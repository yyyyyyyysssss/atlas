package com.atlas.common.core.response;

import com.atlas.common.core.constant.CommonConstant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.MDC;

/**
 * @Description
 * @Author ys
 * @Date 2023/4/28 10:12
 */
public class Result<T> {

    private int code;

    private T data;

    private String message;

    private String requestId;

    public Result() {
    }

    public Result(T data) {
        this(ResultCode.SUCCEED, data);
    }

    public Result(IErrorCode resultCode) {
        this(resultCode.getCode(), null, resultCode.getMessage());
    }

    public Result(IErrorCode resultCode, T data) {
        this(resultCode.getCode(), data, resultCode.getMessage());
    }

    public Result(IErrorCode resultCode, T data, String message) {
        this(resultCode.getCode(), data, message);
    }

    private Result(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.requestId = MDC.get(CommonConstant.TRACE_ID);
    }

    public int getCode() {
        return code;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public String getRequestId() {
        return requestId;
    }

    @JsonIgnore
    public boolean isSucceed(){

        return this.code == ResultCode.SUCCEED.getCode();
    }
}
