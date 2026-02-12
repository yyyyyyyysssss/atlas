package com.atlas.common.core.response;

/**
 * @Description
 * @Author ys
 * @Date 2023/4/28 10:28
 */
public class ResultGenerator {

    public static <T> Result<T> ok(){

        return ok(null);
    }

    public static <T> Result<T> ok(final T data){

        return new Result<>(data);
    }

    public static <T> Result<T> failed(){

        return failed(ResultCode.UNKNOWN_ERROR);
    }

    public static <T> Result<T> failed(IErrorCode resultCode){

        return new Result<>(resultCode, null, resultCode.getMessage());
    }

    public static <T> Result<T> failed(String message){

        return new Result<>(ResultCode.UNKNOWN_ERROR, null, message);
    }

    public static <T> Result<T> failed(IErrorCode resultCode,String message){

        return new Result<>(resultCode, null, message);
    }

    public static <T> Result<T> failedWithDetail(IErrorCode resultCode,T data,String message){

        return new Result<>(resultCode,data,message);
    }

}
