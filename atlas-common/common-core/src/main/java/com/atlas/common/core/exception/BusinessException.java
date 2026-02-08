package com.atlas.common.core.exception;


import com.atlas.common.core.response.ResultCode;
import lombok.Getter;
import lombok.Setter;

import java.util.regex.Matcher;

/**
 * @Description
 * @Author ys
 * @Date 2023/5/11 10:32
 */
@Getter
@Setter
public class BusinessException extends RuntimeException{

    public ResultCode code;

    public String message;

    public BusinessException(ResultCode resultCode){
        this(resultCode,resultCode.getMessage());
    }

    public BusinessException(String reason){
        this(ResultCode.BIZ_ERROR,reason);
    }

    public BusinessException(String reason, Object... args){

        this(ResultCode.BIZ_ERROR,formatWithPlaceholder(reason, args));
    }

    public BusinessException(Throwable throwable){
        this(ResultCode.BIZ_ERROR,throwable.getMessage());
    }

    public BusinessException(ResultCode code, String message){
        this.code=code;
        this.message=message;
    }

    public static String formatWithPlaceholder(String template, Object... args) {
        for (Object arg : args) {
            template = template.replaceFirst("\\{}", Matcher.quoteReplacement(String.valueOf(arg)));
        }
        return template;
    }
}
