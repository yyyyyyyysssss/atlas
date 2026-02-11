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
public class BusinessException extends BaseException{

    public ResultCode code;

    public String message;

    public BusinessException(String reason){
        this(ResultCode.BIZ_ERROR,reason);
    }

    public BusinessException(ResultCode code, String message){
        super(code,message);
        this.code=code;
        this.message=message;
    }

}
